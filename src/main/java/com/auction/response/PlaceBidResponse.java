package com.auction.response;

import com.auction.dto.UserSessionDTO;

public class PlaceBidResponse extends Response {
    private String sessionId;
    private Double currentPrice;
    private String currentWinnerId;
    private String currentWinnerUsername;
    private String status;
    private Double minimumNextBid;
    private UserSessionDTO updatedUser;

    public PlaceBidResponse(boolean success, String message, String sessionId, Double currentPrice,
                            String currentWinnerId, String currentWinnerUsername, String status, Double minimumNextBid,
                            UserSessionDTO updatedUser) {
        super(success, message);
        this.sessionId = sessionId;
        this.currentPrice = currentPrice;
        this.currentWinnerId = currentWinnerId;
        this.currentWinnerUsername = currentWinnerUsername;
        this.status = status;
        this.minimumNextBid = minimumNextBid;
        this.updatedUser = updatedUser;
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

    public Double getMinimumNextBid() {
        return minimumNextBid;
    }

    public UserSessionDTO getUpdatedUser() {
        return updatedUser;
    }

}
