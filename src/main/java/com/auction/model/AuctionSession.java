package com.auction.model;

import java.time.LocalDateTime;

public class AuctionSession extends Entity {

    private static final long serialVersionUID = 1L;

    private Item item;
    private double currentPrice;
    private String currentWinnerId;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status; // "OPEN", "RUNNING", "FINISHED", "CANCELLED"

    public AuctionSession() { super(); }

    public AuctionSession(String id, Item item, LocalDateTime startTime, LocalDateTime endTime) {
        super(id);
        this.item = item;
        this.startTime = startTime;
        this.endTime = endTime;
        this.currentPrice = item.getStartingPrice();
        this.status = "OPEN";
        this.currentWinnerId = null;
    }

    public Item getItem() {
        return item;
    }
    public void setItem(Item item) {
        this.item = item;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }
    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public String getCurrentWinnerId() {
        return currentWinnerId;
    }
    public void setCurrentWinnerId(String currentWinnerId) {
        this.currentWinnerId = currentWinnerId;
    }

    public LocalDateTime getStartTime() {
        return startTime;
    }
    public void setStartTime(LocalDateTime startTime) {
        this.startTime = startTime;
    }

    public LocalDateTime getEndTime() {
        return endTime;
    }
    public void setEndTime(LocalDateTime endTime) {
        this.endTime = endTime;
    }

    public String getStatus() {
        return status;
    }
    public void setStatus(String status) {
        this.status = status;
    }
}