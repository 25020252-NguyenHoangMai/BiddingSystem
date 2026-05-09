package com.auction.response;

public class BidUpdateResponse extends Response {
    private String sessionId;
    private Double currentPrice;
    private String currentWinnerId;
    private String currentWinnerUsername;
    private String status;
    private Long endTimeMillis;

    public BidUpdateResponse(boolean success, String message, String sessionId, Double currentPrice,
                             String currentWinnerId, String currentWinnerUsername, String status, Long endTimeMillis) {
        super(success, message);
        this.sessionId = sessionId;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerUsername = currentWinnerUsername;
        this.status = status;
        this.endTimeMillis = endTimeMillis;
    }

    public String getSessionId() {
        return sessionId;
    }

    public Double getCurrentPrice() {
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

    public Long getEndTimeMillis() {
        return endTimeMillis;
    }
}
