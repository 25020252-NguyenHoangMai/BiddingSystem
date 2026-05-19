package com.auction.request;

public class GetAllUsersRequest extends Request {
    private final String requesterId;

    public GetAllUsersRequest(String requesterId) {
        this.requesterId = requesterId;
    }

    public String getRequesterId() {
        return requesterId;
    }
}
