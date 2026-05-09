package com.auction.server.realtime;

import com.auction.response.BidUpdateResponse;

public interface AuctionSessionObserver {
    boolean onBidUpdated(BidUpdateResponse update);
}
