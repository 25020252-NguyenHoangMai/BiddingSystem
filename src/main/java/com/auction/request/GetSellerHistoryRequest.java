package com.auction.request;

public class GetSellerHistoryRequest extends Request {
    private final String userId;

    public GetSellerHistoryRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }
}
