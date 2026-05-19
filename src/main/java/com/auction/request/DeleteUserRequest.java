package com.auction.request;

public class DeleteUserRequest extends Request {
    private final String userId;

    public DeleteUserRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
