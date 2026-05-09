package com.auction.request;

public class UnwatchSessionRequest extends Request {
    private String sessionId;

    public UnwatchSessionRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
