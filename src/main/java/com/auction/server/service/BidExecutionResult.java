package com.auction.server.service;

public class BidExecutionResult {
    private final boolean success;
    private final String message;
    private final String sessionId;
    private final double currentPrice;
    private final String winnerId;
    private final String status;

    public BidExecutionResult(boolean success, String message, String sessionId, double currentPrice, String winnerId,
                            String status) {
        this.success = success;
        this.message = message;
        this.sessionId = sessionId;
        this.currentPrice = currentPrice;
        this.winnerId = winnerId;
        this.status = status;
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

    public String getWinnerId() {
        return winnerId;
    }

    public String getStatus() {
        return status;
    }
}
