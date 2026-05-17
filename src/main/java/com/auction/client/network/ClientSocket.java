package com.auction.client.network;

import com.auction.response.BidUpdateResponse;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.DashboardWatchResponse;
import com.auction.response.PlaceBidResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.*;

public class ClientSocket {
    private static volatile ClientSocket instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;

    public ClientSocket() {}

    public static ClientSocket getInstance() {
        if (instance == null) {
            synchronized (ClientSocket.class) {
                if (instance == null) {
                    instance = new ClientSocket();
                }
            }
        }
        return instance;
    }

    // Response bình thường cho Request-Response
    private final BlockingQueue<Object> responseQueue  = new LinkedBlockingQueue<>();

    private final BlockingQueue<PlaceBidResponse> placeBidQueue = new LinkedBlockingQueue<>();

    // BidUpdateResponse do server push — không liên quan request nào
    private final BlockingQueue<BidUpdateResponse> bidUpdateQueue = new LinkedBlockingQueue<>();

    // Callback nhận BidUpdateResponse — set bởi AuctionDetailController
    private volatile BidUpdateListener bidUpdateListener;

    // Callback nhận DashboardUpdateResponse — set bởi MainController
    private volatile DashboardUpdateListener dashboardUpdateListener;

    private final BlockingQueue<DashboardWatchResponse> dashboardWatchQueue = new LinkedBlockingQueue<>();

    private final ExecutorService callbackExecutor = Executors.newCachedThreadPool();

    private volatile boolean dashboardWatching = false;

    // Interface Observer — AuctionDetailController implement
    public interface BidUpdateListener {
        void onBidUpdate(BidUpdateResponse update);
    }

    // Interface Observer — MainController implement
    public interface DashboardUpdateListener {
        void onDashboardUpdate(DashboardUpdateResponse update);
    }

    public boolean isDashboardWatching() {
        return dashboardWatching;
    }

    public synchronized void connect() {
        try {
            if (isConnected()) {
                startReaderThread();
                return;
            }

            closeSilently();

            socket = new Socket("127.0.0.1", 5000);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            responseQueue.clear();
            bidUpdateQueue.clear();
            dashboardWatchQueue.clear();
            placeBidQueue.clear();

            startReaderThread();

            System.out.println("Connected to server");
        } catch (Exception e) {
            System.err.println("[ClientSocket] connect failed: " + e.getMessage());
            closeSilently();
        }
    }

    private boolean isConnected() {
        return socket != null && socket.isConnected() && !socket.isClosed()
                && out != null && in != null;
    }

    // ===== READER THREAD — 1 thread duy nhất đọc stream =====
    private synchronized void startReaderThread() {
        if (!isConnected()) { return; }

        if (readerThread != null && readerThread.isAlive()) { return; }

        Thread reader = new Thread(() -> {
            while (isConnected()) {
                try {
                    Object obj = in.readObject();
                    System.out.println("[ClientSocket] received from server: " + obj.getClass().getSimpleName());

                    if (obj instanceof PlaceBidResponse placeBidResponse) {
                        placeBidQueue.offer(placeBidResponse);
                    } else if (obj instanceof BidUpdateResponse update) {
                        BidUpdateListener cb = bidUpdateListener;

                        if (cb != null) {
                            callbackExecutor.execute(() -> cb.onBidUpdate(update));
                        } else {
                            bidUpdateQueue.offer(update);
                        }
                    } else if (obj instanceof DashboardUpdateResponse dashboardUpdate) {
                        DashboardUpdateListener dcb = dashboardUpdateListener;

                        if (dcb != null) {
                            callbackExecutor.execute(() -> dcb.onDashboardUpdate(dashboardUpdate));
                        }
                    } else if (obj instanceof DashboardWatchResponse dwr) {
                        dashboardWatchQueue.offer(dwr);
                    } else {
                        responseQueue.offer(obj);
                    }
                } catch (java.io.EOFException | java.net.SocketException e) {
                    System.out.println("[ClientSocket] Connection closed.");
                    break;
                } catch (Exception e) {
                    System.err.println("[ClientSocket] Reader error: " + e.getMessage());
                    close();
                    break;
                }
            }
        }, "ClientSocket-Reader");
        readerThread = reader;
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public synchronized void sendRequest(Object request) {
        if (!isConnected()) {
            connect();
        }

        if (!isConnected()) {
            throw new IllegalStateException("Socket is not connected");
        }

        try {
            out.writeObject(request);
            out.flush();

            System.out.println("[ClientSocket] sent request: " + request.getClass().getSimpleName());
        } catch (Exception e) {
            System.err.println("[ClientSocket] sendRequest failed: " + e.getMessage());
            close();
        }
    }

    public Object takeResponse() throws Exception {
        Object obj = responseQueue.poll(30, TimeUnit.SECONDS);
        if (obj == null) {
            System.err.println("[ClientSocket] Timeout waiting for server response");
            throw new Exception("Server response timeout");
        }

        System.out.println(
                "[ClientSocket] takeResponse received: "
                        + obj.getClass().getSimpleName());

        return obj;
    }

    public DashboardWatchResponse receiveDashboardWatchResponse() throws Exception {
        DashboardWatchResponse r = dashboardWatchQueue.poll(30, TimeUnit.SECONDS);
        if (r == null) {
            throw new Exception("DashboardWatch response timeout");
        }

        dashboardWatching = r.isSuccess();

        System.out.println("[ClientSocket] dashboardWatching = " + dashboardWatching);

        return r;
    }

    public Object receiveResponse() throws Exception {
        return takeResponse();
    }

    // ===== BID UPDATE LISTENER =====
    // Đăng ký listener — gọi khi mở AuctionDetail
    public void setBidUpdateListener(BidUpdateListener listener) {
        this.bidUpdateListener = listener;

        if (listener != null) {
            BidUpdateResponse pending;
            while ((pending = bidUpdateQueue.poll()) != null) {
                listener.onBidUpdate(pending);
            }
        }
    }

    // Huỷ listener — gọi khi đóng AuctionDetail
    public void clearBidUpdateListener() {
        this.bidUpdateListener = null;
    }

    // ===== DASHBOARD UPDATE LISTENER =====
    public void setDashboardUpdateListener(DashboardUpdateListener listener) {
        this.dashboardUpdateListener = listener;
    }

    // ===== CLOSE =====
    public synchronized void close() {
        // 1. Dừng luồng đọc trước
        if (readerThread != null) {
            readerThread.interrupt();
        }

        // 2. Đóng luồng và socket
        closeSilently();

        // 3. Xóa sạch trạng thái
        readerThread = null;
        responseQueue.clear();
        bidUpdateQueue.clear();
        dashboardWatchQueue.clear();
        placeBidQueue.clear();
        dashboardWatching = false;
        bidUpdateListener = null;
        dashboardUpdateListener = null;
    }

    public void closeSilently() {
        try { if (in  != null) in.close();  } catch (Exception ignored) {}
        try { if (out != null) out.close();  } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}

        in = null;
        out = null;
        socket = null;
    }

    public void clearResponseQueue() {
        responseQueue.clear();
    }

    public PlaceBidResponse takePlaceBidResponse() throws Exception {
        PlaceBidResponse response = placeBidQueue.poll(30, TimeUnit.SECONDS);

        if (response == null) {
            throw new Exception("Server response timeout");
        }

        return response;
    }
}
