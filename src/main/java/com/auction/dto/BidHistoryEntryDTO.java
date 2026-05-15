package com.auction.dto;

import java.io.Serializable;

public class BidHistoryEntryDTO implements Serializable {
    private String bidderUsername;
    private double bidAmount;
    private long bidTimeMillis;

    public BidHistoryEntryDTO(String bidderUsername, double bidAmount, long bidTimeMillis) {
        this.bidderUsername = bidderUsername;
        this.bidAmount = bidAmount;
        this.bidTimeMillis = bidTimeMillis;
    }

    public String getBidderUsername() {
        return bidderUsername;
    }

    public double getBidAmount() {
        return bidAmount;
    }

    public long getBidTimeMillis() {
        return bidTimeMillis;
    }
}
