package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.ItemDTO;
import com.auction.request.PlaceBidRequest;
import com.auction.request.RefreshAuctionRequest;
import com.auction.response.PlaceBidResponse;
import com.auction.response.RefreshAuctionResponse;

public class AuctionService {

    private final ClientSocket socket =
            ClientSocket.getInstance();

    // ===== PLACE BID =====
    public PlaceBidResponse placeBid(
            String sessionId,
            String bidderId,
            double amount
    ) throws Exception {

        PlaceBidRequest request =
                new PlaceBidRequest(
                        sessionId,
                        bidderId,
                        amount
                );

        socket.sendRequest(request);

        Object raw = socket.receiveResponse();

        if (raw instanceof PlaceBidResponse response) {
            return response;
        }

        throw new Exception(
                "Unexpected response from server"
        );
    }

    // ===== REFRESH AUCTION =====
    public ItemDTO refreshAuction(
            String sessionId
    ) throws Exception {

        RefreshAuctionRequest request =
                new RefreshAuctionRequest(sessionId);

        socket.sendRequest(request);

        Object raw = socket.receiveResponse();

        if (raw instanceof RefreshAuctionResponse response) {

            if (response.isSuccess()) {
                return response.getItem();
            }

            return null;
        }

        throw new Exception(
                "Unexpected response from server"
        );
    }
}
