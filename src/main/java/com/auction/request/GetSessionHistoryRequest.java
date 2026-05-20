package com.auction.request;

public class GetSessionHistoryRequest extends Request {
    private final String userId;

    public GetSessionHistoryRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}