package com.auction.client.network;

import com.auction.response.BidUpdateResponse;

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

    // Interface Observer — AuctionDetailController implement
    public interface BidUpdateListener {
        void onBidUpdate(BidUpdateResponse update);
    }

    public void connect() {
        try {
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                return;
            }
            socket = new Socket("127.0.0.1", 5000);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connected to server");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== READER THREAD — 1 thread duy nhất đọc stream =====
    private void startReaderThread() {
        Thread reader = new Thread(() -> {
            while (socket != null && !socket.isClosed()) {
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
        reader.setDaemon(true);
        reader.start();
    }

    public void sendRequest(Object request) {
        try {
            synchronized (out) {
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
    }

    // Huỷ listener — gọi khi đóng AuctionDetail
    public void clearBidUpdateListener() {
        this.bidUpdateListener = null;
    }

    // ===== CLOSE =====
    public void close() {
        try { if (in  != null) in.close();  } catch (Exception ignored) {}
        try { if (out != null) out.close();  } catch (Exception ignored) {}
        try { if (socket != null) socket.close(); } catch (Exception ignored) {}
    }
}
