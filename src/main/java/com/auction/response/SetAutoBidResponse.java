package com.auction.response;

public class SetAutoBidResponse extends Response {
    private String sessionId;
    private String bidderId;
    private Double maxAmount;
    private Double currentPrice;
    private String currentWinnerId;
    private String currentWinnerUsername;
    private String status;

    public SetAutoBidResponse(boolean success, String message, String sessionId, String bidderId, Double maxAmount,
                              Double currentPrice, String currentWinnerId, String currentWinnerUsername,
                              String status) {
        super(success, message);
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.maxAmount = maxAmount;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerUsername = currentWinnerUsername;
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBidderId() {
        return bidderId;
    }

    public Double getMaxAmount() {
        return maxAmount;
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
}
