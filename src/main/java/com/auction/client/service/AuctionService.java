package com.auction.client.service;

import com.auction.client.ClientSession;
import com.auction.client.network.ClientSocket;
import com.auction.request.*;
import com.auction.response.*;

public class AuctionService {

    private final ClientSocket socket =
            ClientSocket.getInstance();

    // ===== PLACE BID =====
    public PlaceBidResponse placeBid(String sessionId, String bidderId, double amount) throws Exception {
        socket.connect();
        socket.sendRequest(new PlaceBidRequest(sessionId, bidderId, amount));

        return socket.takePlaceBidResponse();
    }

    public SessionWatchResponse watchSession(String sessionId, String userID) throws Exception {
        socket.connect();
        String userId = ClientSession.getCurrentUser() != null
                ? ClientSession.getCurrentUser().getId()
                : null;

        socket.sendRequest(new WatchSessionRequest(sessionId, userId));

        Object raw = socket.receiveResponse();

        if (!(raw instanceof SessionWatchResponse response)) {
            throw new IllegalStateException("Expected SessionWatchResponse but got: " + raw);
        }
        socket.clearResponseQueue();
        return response;
    }

    public GetBidHistoryResponse getBidHistory(String sessionId) throws Exception {
        socket.connect();
        socket.sendRequest(new GetBidHistoryRequest(sessionId));

        Object raw = socket.receiveResponse();

        if (!(raw instanceof GetBidHistoryResponse response)) {
            throw new IllegalStateException("Expected GetBidHistoryResponse but got: " + raw);
        }

        return response;
    }

    public void unwatchSession(String sessionId) throws Exception {
        socket.connect();
        socket.sendRequest(new UnwatchSessionRequest(sessionId));
    }

    public SetAutoBidResponse setAutoBid(String sessionId, String bidderId, double maxAmount) throws Exception {
        socket.connect();

        socket.sendRequest(new SetAutoBidRequest(sessionId, bidderId, maxAmount));

        Object raw = socket.receiveResponse();

        if (!(raw instanceof SetAutoBidResponse response)) {
            throw new IllegalStateException(
                    "Expected SetAutoBidResponse but got: " + raw
            );
        }

        return response;
    }
}
