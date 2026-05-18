package com.auction.client.service;

import com.auction.client.ClientSession;
import com.auction.client.network.ClientSocket;
import com.auction.request.*;
import com.auction.response.*;

public class AuctionService {
    private final ClientSocket socket = ClientSocket.getInstance();

    // ===== PLACE BID =====
    public PlaceBidResponse placeBid(String sessionId, String bidderId, double amount) throws Exception {
        socket.connect();
        socket.sendRequest(new PlaceBidRequest(sessionId, bidderId, amount));

        return socket.takePlaceBidResponse();
    }

    public SessionWatchResponse watchSession(String sessionId, String userId) throws Exception {
        socket.connect();

        socket.clearResponseQueue();

        socket.sendRequest(new WatchSessionRequest(sessionId, userId));

        long timeout = System.currentTimeMillis() + 10000;

        while (System.currentTimeMillis() < timeout) {
            Object raw = socket.receiveResponse();

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
            if (!socket.isConnectedPublic()) {
                return;
            }

            socket.sendRequest(new UnwatchSessionRequest(sessionId));
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
            socket.clearBidUpdateListener();
            socket.close();
        } catch (Exception ignored) {}
    }

    public ClientSocket getWatchSocket() {
        return socket;
    }

    public void closeAllSockets() {
        try {
            socket.close();
        } catch (Exception ignored) {}

        closeWatchSocket();
    }
}
