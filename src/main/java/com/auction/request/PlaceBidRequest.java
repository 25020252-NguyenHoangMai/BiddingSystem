package com.auction.request;

public class PlaceBidRequest extends Request {
    private String sessionId;
    private String bidderId;
    private double amount;

    public  PlaceBidRequest(String sessionId, String bidderId, double amount) {
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.amount = amount;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public double getAmount() {
        return amount;
    }
}
