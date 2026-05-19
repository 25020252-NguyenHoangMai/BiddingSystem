package com.auction.request;

public class AdminCancelAuctionRequest extends Request {
    private final String adminId;
    private final String sessionId;

    public AdminCancelAuctionRequest(String adminId, String sessionId) {
        this.adminId = adminId;
        this.sessionId = sessionId;
    }

    public String getAdminId() {
        return adminId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
