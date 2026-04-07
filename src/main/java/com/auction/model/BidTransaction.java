package com.auction.model;

import java.time.LocalDateTime;

public class BidTransaction extends Entity {
    private String auctionId;
    private String bidderId;
    private double bidAmount;
    private LocalDateTime bidTime;

    public BidTransaction() { super(); }

    public BidTransaction(String id, String auctionId, String bidderId, double bidAmount) {
        super(id);
        this.auctionId = auctionId;
        this.bidderId = bidderId;
        this.bidAmount = bidAmount;
        this.bidTime = LocalDateTime.now();
    }

    public String getAuctionId() {
        return auctionId;
    }
    public void setAuctionId(String auctionId) {
        this.auctionId = auctionId;
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