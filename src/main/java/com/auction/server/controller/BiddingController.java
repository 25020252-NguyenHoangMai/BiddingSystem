package com.auction.server.controller;

import com.auction.exception.InsufficientBalanceException;
import com.auction.request.PlaceBidRequest;
import com.auction.response.PlaceBidResponse;
import com.auction.server.service.BiddingService;
import com.auction.server.service.BidResult;

public class BiddingController {
    private final BiddingService biddingService;

    public BiddingController(BiddingService biddingService) {
        this.biddingService = biddingService;
    }

    public PlaceBidResponse placeBid(PlaceBidRequest request) {
        try {
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new PlaceBidResponse(false, "SessionId is required!", null,
                        null, null, null, null);
            }

            if (request.getBidderId() == null || request.getBidderId().isBlank()) {
                return new PlaceBidResponse(false, "BidderId is required!", null,
                        null, null, null, null);
            }

            if (request.getAmount() <= 0) {
                return new PlaceBidResponse(false, "Bid amount must be positive!", null,
                        null, null, null, null);
            }

            BidResult result = biddingService.placeBid(request.getSessionId(),
                    request.getBidderId(), request.getAmount());

            return new PlaceBidResponse(result.isSuccess(), result.getMessage(), result.getSessionId(),
                    result.getCurrentPrice(), result.getCurrentWinnerId(),
                    result.getCurrentWinnerUsername(), result.getStatus());

        } catch (InsufficientBalanceException e) {
            return new PlaceBidResponse(false, e.getMessage(),
                    request.getSessionId(), null, null, null, null);

        } catch (IllegalArgumentException e) {
            return new PlaceBidResponse(false, e.getMessage(),
                    request.getSessionId(), null, null, null, null);

        } catch (Exception e) {
            e.printStackTrace();
            return new PlaceBidResponse(false, "Bid failed: unexpected server error!", null,
                    null, null, null, null);
        }
    }
}
