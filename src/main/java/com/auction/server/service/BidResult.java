package com.auction.server.service;

public class BidResult {
    private final boolean success;
    private final String message;
    private final String sessionId;
    private final double currentPrice;
    private final String currentWinnerId;
    private final String currentWinnerUsername;
    private final String status;
    private final String bidderUsername;
    private final Double bidAmount;
    private final Long bidTimeMillis;

    public BidResult(boolean success, String message, String sessionId, double currentPrice, String currentWinnerId,
                     String currentWinnerUsername, String status) {
        this(success, message, sessionId, currentPrice, currentWinnerId, currentWinnerUsername, status,
                null, null, null);
    }

    public BidResult(boolean success, String message, String sessionId, double currentPrice, String currentWinnerId,
                     String currentWinnerUsername, String status, String bidderUsername, Double bidAmount,
                     Long bidTimeMillis) {
        this.success = success;
        this.message = message;
        this.sessionId = sessionId;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerUsername = currentWinnerUsername;
        this.status = status;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTimeMillis = bidTimeMillis;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }

    public String getSessionId() {
        return sessionId;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public String getCurrentWinnerId() {
        return currentWinnerId;
    }

    public String getCurrentWinnerUsername() {
        return currentWinnerUsername;
    }

    public String getStatus() {
        return status;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public Long getBidTimeMillis() {
        return bidTimeMillis;
    }
}