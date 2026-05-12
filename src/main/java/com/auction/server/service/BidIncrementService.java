package com.auction.server.service;

public class BidIncrementService {

    public double getIncrement(double currentPrice) {

        if (currentPrice < 0) {
            throw new IllegalArgumentException("Current price cannot be negative");
        }

        // Client đang dùng bước tối thiểu = max(10, 1%)
        return Math.max(10.0, currentPrice * 0.01);
    }

    public double getMinimumNextBid(double currentPrice) {
        return currentPrice + getIncrement(currentPrice);
    }
}
