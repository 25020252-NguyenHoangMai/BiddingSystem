package com.auction.request;

public class PlaceBidRequest extends Request {
    private String sessionId;
    private String username;
    private double amount;

    public  PlaceBidRequest(String sessionId, String username, double amount) {
        this.sessionId = sessionId;
        this.username = username;
        this.amount = amount;
    }
}
