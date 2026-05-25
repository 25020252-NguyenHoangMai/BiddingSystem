package com.auction.response;

public class BidUpdateResponse extends Response {
    private String sessionId;
    private Double currentPrice;
    private String currentWinnerId;
    private String currentWinnerUsername;
    private String status;
    private Long endTimeMillis;
    private Double minimumNextBid;
    private String bidderUsername;
    private Double bidAmount;
    private int totalBidsReceived;
    private Long bidTimeMillis;

    public BidUpdateResponse(boolean success, String message, String sessionId, Double currentPrice,
                             String currentWinnerId, String currentWinnerUsername, String status, Long endTimeMillis,
                             Double minimumNextBid, String bidderUsername, Double bidAmount, int totalBidsReceived) {
        this(success, message, sessionId, currentPrice, currentWinnerId, currentWinnerUsername, status,
                endTimeMillis, minimumNextBid, bidderUsername, bidAmount, totalBidsReceived,  null);
    }

    public BidUpdateResponse(boolean success, String message, String sessionId, Double currentPrice,
                             String currentWinnerId, String currentWinnerUsername, String status, Long endTimeMillis,
                             Double minimumNextBid, String bidderUsername, Double bidAmount, int totalBidsReceived, Long bidTimeMillis) {
        super(success, message);
        this.sessionId = sessionId;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerUsername = currentWinnerUsername;
        this.status = status;
        this.endTimeMillis = endTimeMillis;
        this.minimumNextBid = minimumNextBid;
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.totalBidsReceived = totalBidsReceived;
        this.bidTimeMillis = bidTimeMillis;
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

    public String getBidderUsername() {
        return bidderUsername;
    }

    public Double getBidAmount() {
        return bidAmount;
    }

    public int getTotalBidsReceived() {
        return totalBidsReceived;
    }

    public Long getBidTimeMillis() {
        return bidTimeMillis;
    }
}
