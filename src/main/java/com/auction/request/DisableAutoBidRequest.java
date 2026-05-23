package com.auction.request;

public class DisableAutoBidRequest extends Request {
    private final String sessionId;
    private final String bidderId;

    public DisableAutoBidRequest(String sessionId, String bidderId) {
        this.sessionId = sessionId;
        this.bidderId = bidderId;
    }

    public String getSessionId() {
        return sessionId;
    }

    public String getBidderId() {
        return bidderId;
    }
}
