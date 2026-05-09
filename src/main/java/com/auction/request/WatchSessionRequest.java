package com.auction.request;

public class WatchSessionRequest extends Request {
    private String sessionId;

    public WatchSessionRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
