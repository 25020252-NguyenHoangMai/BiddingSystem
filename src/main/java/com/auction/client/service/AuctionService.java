package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.request.PlaceBidRequest;
import com.auction.response.PlaceBidResponse;

public class AuctionService {

    private final ClientSocket socket =
            ClientSocket.getInstance();

    // ===== PLACE BID =====
    public PlaceBidResponse placeBid(String sessionId, String bidderId, double amount) throws Exception {
        socket.connect();
        socket.sendRequest(new PlaceBidRequest(sessionId, bidderId, amount));

        Object raw = socket.takeResponse();

        if (raw instanceof PlaceBidResponse response) { return response; }

        throw new Exception("Unexpected response from server");
    }
}
