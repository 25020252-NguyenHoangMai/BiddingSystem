package com.auction.client.network;

import com.auction.response.BidUpdateResponse;
import com.auction.response.DashboardUpdateResponse;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;

public class ClientSocket {
    private static ClientSocket instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;

    private ClientSocket() {}

    public static ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    // Response bình thường cho Request-Response
    private final BlockingQueue<Object> responseQueue  = new LinkedBlockingQueue<>();

    // BidUpdateResponse do server push — không liên quan request nào
    private final BlockingQueue<BidUpdateResponse> bidUpdateQueue = new LinkedBlockingQueue<>();

    // Callback nhận BidUpdateResponse — set bởi AuctionDetailController
    private volatile BidUpdateListener bidUpdateListener;

    // Callback nhận DashboardUpdateResponse — set bởi MainController
    private volatile DashboardUpdateListener dashboardUpdateListener;

    // Interface Observer — AuctionDetailController implement
    public interface BidUpdateListener {
        void onBidUpdate(BidUpdateResponse update);
    }

    // Interface Observer — MainController implement
    public interface DashboardUpdateListener {
        void onDashboardUpdate(DashboardUpdateResponse update);
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

            startReaderThread();

            System.out.println("Connected to server");

        } catch (Exception e) {
            e.printStackTrace();
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
                    Object obj = in.readObject(); // block ở đây

                    if (obj instanceof BidUpdateResponse update) {
                        // Server push realtime → gọi listener ngay trên reader thread
                        // Listener dùng Platform.runLater nên không block
                        BidUpdateListener cb = bidUpdateListener;
                        if (cb != null) {
                            cb.onBidUpdate(update);
                        } else {
                            bidUpdateQueue.offer(update); // backup nếu chưa có listener
                        }
                    } else if (obj instanceof DashboardUpdateResponse) {
                        // Không cho vào responseQueue tránh bị thread khác lấy nhầm
                    } else {
                        // Response bình thường → bỏ vào queue, AuctionService lấy ra
                        responseQueue.offer(obj);
                    }

                } catch (java.io.EOFException | java.net.SocketException e) {
                    System.out.println("[ClientSocket] Connection closed.");
                    break;
                } catch (Exception e) {
                    if (socket != null && !socket.isClosed()) {
                        System.out.println("[ClientSocket] Reader error: " + e.getMessage());
                    }
                    break;
                }
            }
        }, "ClientSocket-Reader");
        readerThread = reader;
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public void sendRequest(Object request) {
        try {
            connect();

            synchronized (this) {
                if (!isConnected()) {
                    throw new IllegalStateException("Socket is not connected");
                }

                out.writeObject(request);
                out.flush();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object takeResponse() throws Exception {
        Object obj = responseQueue.poll(10, TimeUnit.SECONDS);
        if (obj == null) throw new Exception("Server response timeout");
        return obj;
    }

    public Object receiveResponse() {
        try {
            return takeResponse();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
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
}
