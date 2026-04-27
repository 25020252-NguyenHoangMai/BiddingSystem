package com.auction.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private String sessionId;
    private String bidderId;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction() { super(); }

    public BidTransaction(String id, String sessionId, String bidderId, double bidAmount) {
        super(id);
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public String getSessionId() {
        return sessionId;
    }
    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getBidderId() {
        return bidderId;
    }
    public void setBidderId(String bidderId) {
        this.bidderId = bidderId;
    }

    public double getBidAmount() {
        return bidAmount;
    }
    public void setBidAmount(double bidAmount) {
        this.bidAmount = bidAmount;
    }

    public LocalDateTime getBidTime() {
        return bidTime;
    }
    public void setBidTime(LocalDateTime bidTime) {
        this.bidTime = bidTime;
    }
}