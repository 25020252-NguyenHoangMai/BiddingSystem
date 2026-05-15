package com.auction.request;

public class GetCurrentUserRequest extends Request {
    private final String userId;

    public GetCurrentUserRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
