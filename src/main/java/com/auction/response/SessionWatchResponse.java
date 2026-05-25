package com.auction.response;

public class SessionWatchResponse extends Response {
    private String sessionId;
    private Double currentPrice;
    private String currentWinnerId;
    private String currentWinnerUsername;
    private String status;
    private Long endTimeMillis;
    private Double minimumNextBid;
    private Double availableBalance;
    private Boolean autoBidActive;

    public SessionWatchResponse(boolean success, String message) {
        this(success, message, null, null, null, null, null,
                null, null, null, null);
    }

    public SessionWatchResponse(boolean success, String message, String sessionId, Double currentPrice,
                                String currentWinnerId, String currentWinnerUsername, String status,
                                Long endTimeMillis, Double minimumNextBid, Double availableBalance,
                                Boolean autoBidActive) {
        super(success, message);
        this.sessionId = sessionId;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerUsername = currentWinnerUsername;
        this.status = status;
        this.endTimeMillis = endTimeMillis;
        this.minimumNextBid = minimumNextBid;
        this.availableBalance = availableBalance;
        this.autoBidActive = autoBidActive;
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

    public Double getMinimumNextBid() {
        return minimumNextBid;
    }

    public Double getAvailableBalance() {
        return availableBalance;
    }

    public Boolean getAutoBidActive() {
        return autoBidActive;
    }

    public boolean isAutoBidActive() {
        return Boolean.TRUE.equals(autoBidActive);
    }
}
