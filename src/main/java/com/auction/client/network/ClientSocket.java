package com.auction.client.network;

import com.auction.protocol.EventMessage;
import com.auction.protocol.RequestMessage;
import com.auction.protocol.ResponseMessage;
import com.auction.request.Request;
import com.auction.response.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
import java.util.Properties;
import java.util.UUID;
import java.util.concurrent.*;

public class ClientSocket {
    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;
    private Thread readerThread;

    private static final ClientSocket INSTANCE = new ClientSocket();

    public static ClientSocket getInstance() {
        return INSTANCE;
    }

    private ClientSocket() {}

    private final ConcurrentHashMap<String, BidUpdateListener> bidUpdateListeners = new ConcurrentHashMap<>();

    // Callback nhận DashboardUpdateResponse — set bởi MainController
    private volatile DashboardUpdateListener dashboardUpdateListener;

    private final ConcurrentHashMap<String, CompletableFuture<Response>> pendingRequests = new ConcurrentHashMap<>();

    private ExecutorService callbackExecutor = Executors.newCachedThreadPool();

    private volatile boolean dashboardWatching = false;

    private final Object writeLock = new Object();

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

    private boolean isSocketAlive() {
        return socket != null
                && socket.isConnected()
                && !socket.isClosed()
                && !socket.isInputShutdown()
                && !socket.isOutputShutdown()
                && out != null
                && in != null;
    }

    private String getServerHost() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            Properties props = new Properties();

            if (input == null) {
                System.err.println("[ClientSocket] config.properties not found, using 127.0.0.1");
                return "127.0.0.1";
            }

            props.load(input);
            return props.getProperty("server.host", "127.0.0.1");
        } catch (Exception e) {
            System.err.println("[ClientSocket] Cannot read server.host: " + e.getMessage());
            return "127.0.0.1";
        }
    }

    private int getServerPort() {
        try (InputStream input = getClass().getClassLoader().getResourceAsStream("config.properties")) {
            Properties props = new Properties();

            if (input == null) {
                System.err.println("[ClientSocket] config.properties not found, using 5000");
                return 5000;
            }

            props.load(input);
            return Integer.parseInt(props.getProperty("server.port", "5000"));
        } catch (Exception e) {
            System.err.println("[ClientSocket] Cannot read server.port: " + e.getMessage());
            return 5000;
        }
    }

    public synchronized void connect() {
        try {
            if (isConnected()) {
                startReaderThread();
                return;
            }

            closeSilently();

            if (callbackExecutor == null || callbackExecutor.isShutdown()) {
                callbackExecutor = Executors.newCachedThreadPool();
            }

            String host = getServerHost();
            int port = getServerPort();

            System.out.println("[ClientSocket] Connecting to server: " + host + ":" + port);

            socket = new Socket();

            socket.connect(new java.net.InetSocketAddress(host, port), 5000);

            socket.setKeepAlive(true);
            socket.setTcpNoDelay(true);

            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();
            in = new ObjectInputStream(socket.getInputStream());

            startReaderThread();

            System.out.println("[ClientSocket] Connected to server: " + host + ":" + port);
        } catch (Exception e) {
            closeSilently();
            throw new RuntimeException("[ClientSocket] connect failed", e);
        }
    }

    private boolean isConnected() {
        return isSocketAlive()
                && readerThread != null
                && readerThread.isAlive();
    }

    // ===== READER THREAD — 1 thread duy nhất đọc stream =====
    private synchronized void startReaderThread() {
        if (!isSocketAlive()) { return; }

        if (readerThread != null && readerThread.isAlive()) { return; }

        Thread reader = new Thread(() -> {
            while (isSocketAlive()) {
                try {
                    Object obj = in.readObject();

                    if (obj instanceof ResponseMessage responseMessage) {
                        String requestId = responseMessage.getRequestId();

                        CompletableFuture<Response> future = pendingRequests.remove(requestId);

                        if (future != null) {
                            future.complete(responseMessage.getPayload());
                        }

                    } else if (obj instanceof EventMessage eventMessage) {
                        handleEvent(eventMessage.getPayload());

                    } else {

                        throw new IOException("Unknown protocol message: " + obj.getClass().getName());
                    }
                } catch (java.io.EOFException | java.net.SocketException e) {
                    System.out.println("[ClientSocket] Connection closed.");
                    handleDisconnect();
                    break;
                } catch (Exception e) {
                    System.err.println("[ClientSocket] Reader error: " + e.getMessage());
                    handleDisconnect();
                    break;
                }
            }
        }, "ClientSocket-Reader");
        readerThread = reader;
        readerThread.setDaemon(true);
        readerThread.start();
    }

    public synchronized void sendRequest(Request request) {
        try {
            if (!isConnected()) {
                throw new IllegalStateException("Socket is not connected");
            }

            String requestId = UUID.randomUUID().toString();
            RequestMessage message = new RequestMessage(requestId, request);

            synchronized (writeLock) {
                out.writeObject(message);
                out.flush();
            }

            System.out.println("[ClientSocket] sent request: "
                    + request.getClass().getSimpleName());

        } catch (Exception e) {
            System.err.println("[ClientSocket] sendRequest failed: "
                    + e.getMessage());

            handleDisconnect();

            throw new RuntimeException("[ClientSocket] sendRequest failed", e);
        }
    }

    public <T extends Response> T sendRequestAndWait(Request request, Class<T> expectedType) throws Exception {
        synchronized (writeLock) {
            if (!isConnected()) {
                connect();
            }
        }

        String requestId = UUID.randomUUID().toString();

        CompletableFuture<Response> future = new CompletableFuture<>();

        pendingRequests.put(requestId, future);

        try {
            RequestMessage message = new RequestMessage(requestId, request);

            synchronized (writeLock) {
                out.writeObject(message);
                out.flush();
            }

            Response response = future.get(30, TimeUnit.SECONDS);

            if (!expectedType.isInstance(response)) {
                throw new IOException(
                        "Expected "
                                + expectedType.getSimpleName()
                                + " but got "
                                + response.getClass().getSimpleName()
                );
            }

            return expectedType.cast(response);

        } catch (Exception e) {
            handleDisconnect();
            throw e;

        } finally {
            pendingRequests.remove(requestId);
        }
    }

    // ===== BID UPDATE LISTENER =====
    public void setBidUpdateListener(String sessionId, BidUpdateListener listener) {
        bidUpdateListeners.put(sessionId, listener);
    }
    // Huỷ listener — gọi khi đóng AuctionDetail
    public void clearBidUpdateListener(String sessionId) {
        bidUpdateListeners.remove(sessionId);
    }

    // ===== DASHBOARD UPDATE LISTENER =====
    public void setDashboardUpdateListener(DashboardUpdateListener listener) {
        this.dashboardUpdateListener = listener;
        if (listener != null) {
            this.dashboardWatching = true;
        }
    }

    // ===== CLOSE =====
    public synchronized void close() {
        for (CompletableFuture<Response> future : pendingRequests.values()) {
            future.completeExceptionally(new IOException("Socket closed"));
        }

        pendingRequests.clear();

        // 1. Dừng luồng đọc trước
        if (readerThread != null) {
            readerThread.interrupt();
        }

        callbackExecutor.shutdownNow();

        // 2. Đóng luồng và socket
        closeSilently();

        // 3. Xóa sạch trạng thái
        readerThread = null;
        dashboardWatching = false;
        bidUpdateListeners.clear();
        dashboardUpdateListener = null;
    }

    public void closeSilently() {
        synchronized (writeLock) {
            try {
                if (in != null) { in.close(); }
            } catch (Exception ignored) {}

            try {
                if (out != null) { out.close(); }
            } catch (Exception ignored) {}

            try {
                if (socket != null) { socket.close(); }
            } catch (Exception ignored) {}

            in = null;
            out = null;
            socket = null;
        }
    }

    private synchronized void handleDisconnect() {
        for (CompletableFuture<Response> future : pendingRequests.values()) {
            future.completeExceptionally(new IOException("Connection lost"));
        }

        pendingRequests.clear();

        closeSilently();

        if (readerThread != null) {
            readerThread.interrupt();
            readerThread = null;
        }

        dashboardWatching = false;
    }

    public boolean isConnectedPublic() {
        return isConnected();
    }

    private void handleEvent(Object payload) {
        if (payload instanceof BidUpdateResponse bidUpdate) {
            dispatchBidUpdate(bidUpdate);

        } else if (payload instanceof DashboardUpdateResponse dashboardUpdate) {
            dispatchDashboardUpdate(dashboardUpdate);

        } else {
            System.err.println("Unknown event payload: " + payload.getClass().getName());
        }
    }

    private void dispatchBidUpdate(BidUpdateResponse update) {
        BidUpdateListener listener = bidUpdateListeners.get(update.getSessionId());

        if (listener == null) {
            return;
        }

        callbackExecutor.execute(() -> {
            try {
                listener.onBidUpdate(update);
            } catch (Exception e) {
                System.err.println(
                        "[ClientSocket] BidUpdate listener error: "
                                + e.getMessage()
                );
            }
        });
    }

    private void dispatchDashboardUpdate(DashboardUpdateResponse update) {
        DashboardUpdateListener listener = dashboardUpdateListener;

        if (listener == null) {
            return;
        }

        callbackExecutor.execute(() -> {
            try {
                listener.onDashboardUpdate(update);
            } catch (Exception e) {
                System.err.println("[ClientSocket] Dashboard listener error: " + e.getMessage());
            }
        });
    }
}
