package com.auction.request;

public class SellerUpdateAuctionTimeRequest extends Request {
    private final String sellerId;
    private final String sessionId;
    private final long endTimeMillis;

    public SellerUpdateAuctionTimeRequest(String sellerId, String sessionId, long endTimeMillis) {
        this.sellerId = sellerId;
        this.sessionId = sessionId;
        this.endTimeMillis = endTimeMillis;
    }

    public String getSellerId() {
        return sellerId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getEndTimeMillis() {
        return endTimeMillis;
    }
}
