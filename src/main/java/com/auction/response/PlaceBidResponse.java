package com.auction.response;

public class PlaceBidResponse extends Response {
    private String sessionId;
    private Double currentPrice;
    private String currentWinnerId;
    private String currentWinnerUsername;
    private String status;

    public PlaceBidResponse(boolean success, String message, String sessionId, Double currentPrice,
                            String currentWinnerId, String currentWinnerUsername, String status) {
        super(success, message);
        this.sessionId = sessionId;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerUsername = currentWinnerUsername;
        this.status = status;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getCurrentWinnerId() {
        return currentWinnerId;
    }

    public String getCurrentWinnerUsername() {
        return currentWinnerUsername;
    }

    public Double getCurrentPrice() {
        return currentPrice;
    }

    public String getStatus() {
        return status;
    }

}
