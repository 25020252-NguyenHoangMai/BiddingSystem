package com.auction.request;

public class SellerCancelAuctionRequest extends Request {
    private final String sellerId;
    private final String sessionId;

    public SellerCancelAuctionRequest(String sellerId, String sessionId) {
        this.sellerId = sellerId;
        this.sessionId = sessionId;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
