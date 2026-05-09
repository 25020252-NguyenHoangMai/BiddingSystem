package com.auction.request;

public class EnableSellerRequest extends Request{
    private final String userId;

    public EnableSellerRequest(String userId) {
        this.userId = userId;
    }

    public String getUserId() { return userId; }
}
