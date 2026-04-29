package com.auction.server.service;

public class BidIncrementService {
    private static final double TIER_ONE_LIMIT = 100_000;
    private static final double TIER_TWO_LIMIT = 1_000_000;
    private static final double TIER_THREE_LIMIT = 10_000_000;

    private static final double TIER_ONE_INCREMENT = 5_000;
    private static final double TIER_TWO_INCREMENT = 20_000;
    private static final double TIER_THREE_INCREMENT = 100_000;
    private static final double TIER_FOUR_INCREMENT = 200_000;

    public double getIncrement(double currentPrice) {
        if (currentPrice < 0) {
            throw new IllegalArgumentException("Current price cannot be negative");
        }

        else if (currentPrice < TIER_ONE_LIMIT) {
            return  TIER_ONE_INCREMENT;
        }
        else if (currentPrice < TIER_TWO_LIMIT) {
            return  TIER_TWO_INCREMENT;
        }
        else if (currentPrice < TIER_THREE_LIMIT) {
            return  TIER_THREE_INCREMENT;
        }

        return  TIER_FOUR_INCREMENT;
    }

    public double getMinimumNextBid(double currentPrice) {
        return currentPrice + getIncrement(currentPrice);
    }
}
