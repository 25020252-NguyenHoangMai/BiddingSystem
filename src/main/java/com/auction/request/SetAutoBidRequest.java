package com.auction.request;

public class SetAutoBidRequest extends Request {
    private final String sessionId;
    private final String bidderId;
    private final double maxAmount;

    public SetAutoBidRequest(String sessionId, String bidderId, double maxAmount) {
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getMaxAmount() {
        return maxAmount;
    }
}

