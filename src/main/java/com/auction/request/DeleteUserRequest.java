package com.auction.request;

public class DeleteUserRequest extends Request {
    private final String requesterId;
    private final String targetUserId;

    public DeleteUserRequest(String requesterId, String targetUserId) {
        this.requesterId = requesterId;
        this.targetUserId = targetUserId;
    }

    public String getRequesterId() {
        return requesterId;
    }

    public String getTargetUserId() {
        return targetUserId;
    }
}
