package com.auction.request;

public class GetAuctionDetailRequest extends Request {
    private final String sessionId;

    public GetAuctionDetailRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
