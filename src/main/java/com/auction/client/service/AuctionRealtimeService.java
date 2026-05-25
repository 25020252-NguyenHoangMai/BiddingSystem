package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.response.BidUpdateResponse;
import com.auction.response.SessionWatchResponse;

public class AuctionRealtimeService implements ClientSocket.BidUpdateListener {
    public interface AuctionUpdateListener {
        void onAuctionUpdated(BidUpdateResponse update);
    }

    private final AuctionService auctionService;
    private volatile AuctionUpdateListener listener;

    public AuctionRealtimeService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void setListener(AuctionUpdateListener listener) {
        this.listener = listener;
    }

    public SessionWatchResponse watch(String sessionId, String userId) throws Exception {
        if (sessionId == null || sessionId.isBlank()) {
            throw new Exception("Session ID is required");
        }

        SessionWatchResponse response = auctionService.watchSession(sessionId, userId);

        if (response == null || !response.isSuccess()) {
            throw new Exception(
                    response != null
                            ? response.getMessage()
                            : "Failed to watch session"
            );
        }

        ClientSocket.getInstance().setBidUpdateListener(sessionId, this);
        return response;
    }

    public void unwatch(String sessionId) {
        if (sessionId == null) return;
        try {
            auctionService.unwatchSession(sessionId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        ClientSocket.getInstance().clearBidUpdateListener(sessionId, this);
    }

    @Override
    public void onBidUpdate(BidUpdateResponse update) {
        if (listener != null) {
            listener.onAuctionUpdated(update);
        }
    }
}
