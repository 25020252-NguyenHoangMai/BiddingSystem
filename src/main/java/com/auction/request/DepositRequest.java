package com.auction.request;

public class DepositRequest extends Request {
    private final String userId;
    private final double amount;

    public DepositRequest(String userId, double amount) {
        this.userId = userId;
        this.amount = amount;
    }

    public String getUserId() {
        return userId;
    }
    public double getAmount() {
        return amount;
    }
}
