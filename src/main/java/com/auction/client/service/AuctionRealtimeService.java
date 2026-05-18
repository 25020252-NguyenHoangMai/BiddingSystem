package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.client.service.AuctionService;
import com.auction.response.BidUpdateResponse;
import com.auction.response.SessionWatchResponse;

public class AuctionRealtimeService implements ClientSocket.BidUpdateListener {
    public interface AuctionUpdateListener {
        void onAuctionUpdated(BidUpdateResponse update);
    }

    private final AuctionService auctionService;
    private AuctionUpdateListener listener;

    public AuctionRealtimeService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public void setListener(AuctionUpdateListener listener) {
        this.listener = listener;
    }

    public SessionWatchResponse watch(String sessionId, String userId) throws Exception {
        SessionWatchResponse response = auctionService.watchSession(sessionId, userId);

        if (response == null || !response.isSuccess()) {
            throw new Exception(
                    response != null
                            ? response.getMessage()
                            : "Failed to watch session"
            );
        }

        auctionService.getWatchSocket().setBidUpdateListener(this);
        return response;
    }

    public void unwatch(String sessionId) {
        try {
            auctionService.unwatchSession(sessionId);
        } catch (Exception ex) {
            ex.printStackTrace();
        }

        auctionService.getWatchSocket().clearBidUpdateListener();
    }

    @Override
    public void onBidUpdate(BidUpdateResponse update) {
        if (listener != null) {
            listener.onAuctionUpdated(update);
        }
    }
}
