package com.auction.response;

public class DisableAutoBidResponse extends Response {
    private final String sessionId;
    private final String bidderId;
    private final boolean autoBidActive;
    private final Double currentPrice;
    private final String currentWinnerId;
    private final String currentWinnerUsername;
    private final String status;

    public DisableAutoBidResponse(boolean success, String message, String sessionId, String bidderId,
                                  boolean autoBidActive, Double currentPrice, String currentWinnerId,
                                  String currentWinnerUsername, String status) {
        super(success, message);
        this.sessionId = sessionId;
        this.bidderId = bidderId;
        this.autoBidActive =  autoBidActive;
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

    public boolean isAutoBidActive() {
        return autoBidActive;
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
