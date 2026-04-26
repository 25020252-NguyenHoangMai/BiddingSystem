package com.auction.server.controller;

import com.auction.server.service.BiddingService;

public class BiddingController {
    private final BiddingService biddingService;

    public BiddingController(BiddingService biddingService) {
        this.biddingService = biddingService;
    }
}
