package com.auction.client.service;

import com.auction.client.ClientSession;
import com.auction.client.network.ClientSocket;
import com.auction.request.*;
import com.auction.response.*;

public class AuctionService {
    private final ClientSocket socket = new ClientSocket();
    private final ClientSocket watchSocket = new ClientSocket();

    // ===== PLACE BID =====
    public PlaceBidResponse placeBid(String sessionId, String bidderId, double amount) throws Exception {
        socket.connect();
        socket.sendRequest(new PlaceBidRequest(sessionId, bidderId, amount));

        return socket.takePlaceBidResponse();
    }

    public SessionWatchResponse watchSession(String sessionId, String userId) throws Exception {
        watchSocket.connect();

        watchSocket.clearResponseQueue();

        watchSocket.sendRequest(new WatchSessionRequest(sessionId, userId));

        long timeout = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < timeout) {
            Object raw = watchSocket.receiveResponse();

            if (raw instanceof SessionWatchResponse response) {
                return response;
            }

            System.out.println(
                    "[AuctionService] Skip unexpected response: "
                            + raw.getClass().getSimpleName()
            );
        }

        throw new Exception("Watch session timeout");
    }

    public GetBidHistoryResponse getBidHistory(String sessionId) throws Exception {
        socket.connect();
        socket.sendRequest(new GetBidHistoryRequest(sessionId));

        Object raw = socket.receiveResponse();

        if (!(raw instanceof GetBidHistoryResponse response)) {
            throw new IllegalStateException("Expected GetBidHistoryResponse but got: " + (raw == null ? "null" : raw.getClass().getName()));
        }

        return response;
    }

    public void unwatchSession(String sessionId) {
        try {
            if (!watchSocket.isConnectedPublic()) {
                return;
            }

            watchSocket.sendRequest(new UnwatchSessionRequest(sessionId));
        } catch (Exception ignored) {}
    }

    public SetAutoBidResponse setAutoBid(String sessionId, String bidderId, double maxAmount) throws Exception {
        socket.connect();

        socket.sendRequest(new SetAutoBidRequest(sessionId, bidderId, maxAmount));

        Object raw = socket.receiveResponse();

        if (!(raw instanceof SetAutoBidResponse response)) {
            throw new IllegalStateException(
                    "Expected SetAutoBidResponse but got: " + (raw == null ? "null" : raw.getClass().getName()));
        }

        return response;
    }

    public void closeWatchSocket() {
        try {
            watchSocket.clearBidUpdateListener();
            watchSocket.close();
        } catch (Exception ignored) {}
    }

    public ClientSocket getWatchSocket() {
        return watchSocket;
    }

    public void closeAllSockets() {
        try {
            socket.close();
        } catch (Exception ignored) {}

        closeWatchSocket();
    }
}
