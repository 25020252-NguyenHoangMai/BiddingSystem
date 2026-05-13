package com.auction.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class BidReservationCalculatorTest {

    private BidReservationCalculator calculator;

    @BeforeEach
    void setUp() {

        calculator = new BidReservationCalculator();
    }


    @Nested
    class CalculateReserveChange {

        @Test
        void firstBid_ReturnsFullAmount() {

            double result = calculator.calculateReserveChange(null, "user1", 0.0, 150.0);
            assertEquals(150.0, result, 0.0001);
        }

        @Test
        void differentBidder_ReturnsFullAmount() {

            double result = calculator.calculateReserveChange("user1", "user2", 100.0, 150.0);
            assertEquals(150.0, result, 0.0001);
        }

        @Test
        void sameBidder_ReturnsDifference() {

            double result = calculator.calculateReserveChange("user1", "user1", 100.0, 150.0);
            assertEquals(50.0, result, 0.0001);
        }
    }
}