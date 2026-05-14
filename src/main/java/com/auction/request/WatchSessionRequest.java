package com.auction.request;

public class WatchSessionRequest extends Request {
    private String sessionId;
    private String userId;

    public WatchSessionRequest(String sessionId, String userID) {
        this.sessionId = sessionId;
        this.userId = userID;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getUserId() {
        return userId;
    }
}
