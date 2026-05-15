package com.auction.request;

public class GetBidHistoryRequest extends Request {
    private final String sessionId;

    public GetBidHistoryRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
