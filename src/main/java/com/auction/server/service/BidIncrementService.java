package com.auction.server.service;

public class BidIncrementService {
    private static final double MIN_INCREMENT = 5_000;
    private static final double INCREMENT_RATE = 0.01;

    public double getIncrement(double currentPrice) {
        if (currentPrice < 0) {
            throw new IllegalArgumentException("Current price cannot be negative");
        }

        // Client đang dùng bước tối thiểu = max(10, 1%)
        return Math.max(MIN_INCREMENT, currentPrice * INCREMENT_RATE);
    }

    public double getMinimumNextBid(double currentPrice) {
        return currentPrice + getIncrement(currentPrice);
    }
}
