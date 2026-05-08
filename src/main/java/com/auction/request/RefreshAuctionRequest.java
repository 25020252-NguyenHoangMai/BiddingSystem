package com.auction.request;

import java.io.Serializable;

public class RefreshAuctionRequest implements Serializable {

    private final String sessionId;

    public RefreshAuctionRequest(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getSessionId() {
        return sessionId;
    }
}
