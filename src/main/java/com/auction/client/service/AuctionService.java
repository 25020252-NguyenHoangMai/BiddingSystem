package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.request.PlaceBidRequest;
import com.auction.request.UnwatchSessionRequest;
import com.auction.request.WatchSessionRequest;
import com.auction.response.BidUpdateResponse;
import com.auction.response.PlaceBidResponse;
import com.auction.response.SessionWatchResponse;

public class AuctionService {

    private final ClientSocket socket =
            ClientSocket.getInstance();

    // ===== PLACE BID =====
    public PlaceBidResponse placeBid(String sessionId, String bidderId, double amount) throws Exception {
        socket.connect();
        socket.sendRequest(new PlaceBidRequest(sessionId, bidderId, amount));

        return socket.takePlaceBidResponse();
    }

    public SessionWatchResponse watchSession(String sessionId) throws Exception {
        socket.connect();
        socket.sendRequest(new WatchSessionRequest(sessionId));

        Object raw = socket.receiveResponse();

        if (!(raw instanceof SessionWatchResponse response)) {
            throw new IllegalStateException("Expected SessionWatchResponse but got: " + raw);
        }

        return response;
    }

    public void unwatchSession(String sessionId) throws Exception {
        socket.connect();
        socket.sendRequest(new UnwatchSessionRequest(sessionId));
    }
}
