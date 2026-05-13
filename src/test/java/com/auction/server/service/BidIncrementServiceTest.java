package com.auction.server.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.*;

class BidIncrementServiceTest {

    private BidIncrementService bidIncrementService;

    @BeforeEach
    void setUp() {

        bidIncrementService = new BidIncrementService();
    }

    //test ném exception
    @Nested
    class TestGetIncrement {
        @Test
        void NegativePrice_ThrowsException() {
            IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
                bidIncrementService.getIncrement(-10.0);
            });
            assertEquals("Current price cannot be negative", exception.getMessage());
        }

        //nhánh1: gtrị 1% <= MIN_INCREMENT = 1.0
        @ParameterizedTest
        @CsvSource({
                "0.0, 1.0",     // 1% là 0 < 1.0 =>pk trả về 1.0
                "50.0, 1.0",    // 1% là 0.5 < 1.0 =>pk trả về 1.0
                "99.9, 1.0",    // 1% là 0.999 < 1.0 =>pk trả về 1.0
                "100.0, 1.0"    // 1% là 1 = 1.0 =>pk trả về 1.0
        })
        void BelowOrEqualThreshold_ReturnsMinIncrement(double currentPrice, double expectedIncrement) {
            double actualIncrement = bidIncrementService.getIncrement(currentPrice);

            assertEquals(expectedIncrement, actualIncrement, 0.0001);
        }

        //nhánh 2: gtrị 1% >= MIN_INCREMENT = 1.0

        @ParameterizedTest
        @CsvSource({
                "101.0, 1.01",  // 1% là 1.01 > 1.0 => Lấy 1.01
                "500.0, 5.0",   // 1% là 5.0 > 1.0 => Lấy 5.0
                "1000.0, 10.0", // 1% là 10.0 > 1.0 => Lấy 10.0
                "9999.0, 99.99"
        })
        void AboveThreshold_ReturnsPercentage(double currentPrice, double expectedIncrement) {
            double actualIncrement = bidIncrementService.getIncrement(currentPrice);
            assertEquals(expectedIncrement, actualIncrement, 0.0001);
        }
    }

    // test hàm tính tổng gtri
    @ParameterizedTest
    @CsvSource({
            "50.0, 51.0",
            "100.0, 101.0",
            "1000.0, 1010.0"
    })
    void testGetMinimumNextBid_CalculatesCorrectly(double currentPrice, double expectedNextBid) {
        double actualNextBid = bidIncrementService.getMinimumNextBid(currentPrice);
        assertEquals(expectedNextBid, actualNextBid, 0.0001);
    }
}