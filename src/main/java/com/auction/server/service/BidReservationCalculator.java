package com.auction.server.service;

public class BidReservationCalculator {
    public double calculateReserveChange(String currentWinnerId, String bidderId, double currentPrice,
                                         double bidAmount) {
        if (currentWinnerId != null && currentWinnerId.equals(bidderId)) {
            return bidAmount - currentPrice;
        }

        return bidAmount;
    }
}
