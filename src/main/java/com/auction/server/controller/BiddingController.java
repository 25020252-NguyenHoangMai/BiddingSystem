package com.auction.server.controller;

import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionSession;
import com.auction.request.PlaceBidRequest;
import com.auction.request.SetAutoBidRequest;
import com.auction.response.BidUpdateResponse;
import com.auction.response.PlaceBidResponse;
import com.auction.response.SetAutoBidResponse;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.AutoBiddingService;
import com.auction.server.service.BiddingService;
import com.auction.server.service.BidResult;
import com.auction.server.service.SessionService;

import java.time.ZoneId;
import java.util.List;

public class BiddingController {
    private final BiddingService biddingService;
    private final SessionService sessionService;
    private final SessionWatchRegistry sessionWatchRegistry;
    private final AutoBiddingService autoBiddingService;

    public BiddingController(BiddingService biddingService, SessionService sessionService,
                             SessionWatchRegistry sessionWatchRegistry, AutoBiddingService autoBiddingService) {
        this.biddingService = biddingService;
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.sessionService = sessionService;
        this.autoBiddingService = autoBiddingService;
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

            BidResult result = biddingService.placeBid(request.getSessionId(), request.getBidderId(),
                                                request.getAmount());

            if (result.isSuccess()) {
                broadcastBidUpdate(result);

                List<BidResult> autoBidResults = autoBiddingService.processAutoBidsAfterBid(
                        result.getSessionId(),
                        request.getBidderId()
                );

                for (BidResult autoBidResult : autoBidResults) {
                    if (autoBidResult.isSuccess()) {
                        broadcastBidUpdate(autoBidResult);
                    }
                }
            }

            return new PlaceBidResponse(
                            result.isSuccess(),
                            result.getMessage(),
                            result.getSessionId(),
                            result.getCurrentPrice(),
                            result.getCurrentWinnerId(),
                            result.getCurrentWinnerUsername(),
                            result.getStatus()
                    );

            //return new PlaceBidResponse(result.isSuccess(), result.getMessage(), result.getSessionId(),
                    //result.getCurrentPrice(), result.getCurrentWinnerId(),
                    //result.getCurrentWinnerUsername(), result.getStatus());

        } catch (InsufficientBalanceException e) {
            return new PlaceBidResponse(false, e.getMessage(),
                    request.getSessionId(), null, null, null, null);
        } catch (InvalidBidException | IllegalArgumentException e) {
            return new PlaceBidResponse(false, e.getMessage(),
                    request.getSessionId(), null, null, null, null);
        } catch (Exception e) {
            System.out.println("=== PLACE BID ERROR ===");
            e.printStackTrace();
            return new PlaceBidResponse(false, "Bid failed: unexpected server error!", null,
                    null, null, null, null);
        }
    }

    private void broadcastBidUpdate(BidResult result) {
        try {
            Long endTimeMillis = getEndTimeMillis(result.getSessionId());

            BidUpdateResponse update = new BidUpdateResponse(
                    true,
                    result.getMessage(),
                    result.getSessionId(),
                    result.getCurrentPrice(),
                    result.getCurrentWinnerId(),
                    result.getCurrentWinnerUsername(),
                    result.getStatus(),
                    endTimeMillis
            );

            sessionWatchRegistry.broadcastBidUpdate(result.getSessionId(), update);
        } catch (Exception e) {
            System.out.println("Failed to broadcast bid update for session: " + result.getSessionId());
            e.printStackTrace();
        }
    }

    private Long getEndTimeMillis(String sessionId) {
        AuctionSession session = sessionService.getSession(sessionId);

        if (session == null || session.getEndTime() == null) {
            return null;
        }

        return session.getEndTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();
    }

    public SetAutoBidResponse setAutoBid(SetAutoBidRequest request) {
        try {
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new SetAutoBidResponse(false, "SessionId is required!",
                        null, request.getBidderId(), request.getMaxAmount(), null,
                        null, null, null);
            }

            if (request.getBidderId() == null || request.getBidderId().isBlank()) {
                return new SetAutoBidResponse(false, "BidderId is required!",
                        request.getSessionId(), null, request.getMaxAmount(), null,
                        null, null, null);
            }

            if (request.getMaxAmount() <= 0) {
                return new SetAutoBidResponse(false, "Auto bid max amount must be positive!",
                        request.getSessionId(), request.getBidderId(), request.getMaxAmount(),
                        null, null, null, null);
            }

            BidResult result = autoBiddingService.setAutoBid(request.getSessionId(), request.getBidderId(),
                                                        request.getMaxAmount());

            if (result.isSuccess()) {
                broadcastBidUpdate(result);

                List<BidResult> autoBidResults = autoBiddingService.processAutoBidsAfterBid(
                        result.getSessionId(),
                        request.getBidderId()
                );

                for (BidResult autoBidResult : autoBidResults) {
                    if (autoBidResult.isSuccess()) {
                        broadcastBidUpdate(autoBidResult);
                    }
                }
            }

            return new SetAutoBidResponse(result.isSuccess(), result.getMessage(), result.getSessionId(),
                        request.getBidderId(), request.getMaxAmount(), result.getCurrentPrice(),
                        result.getCurrentWinnerId(), result.getCurrentWinnerUsername(), result.getStatus());
        } catch (InsufficientBalanceException e) {
            return new SetAutoBidResponse(false, e.getMessage(), request.getSessionId(), request.getBidderId(),
                    request.getMaxAmount(), null, null, null, null);

        } catch (InvalidBidException | IllegalArgumentException e) {
            return new SetAutoBidResponse(false, e.getMessage(), request.getSessionId(), request.getBidderId(),
                    request.getMaxAmount(), null, null, null, null);

        } catch (Exception e) {
            e.printStackTrace();
            return new SetAutoBidResponse(false, "Auto bid failed: unexpected server error!",
                    request.getSessionId(), request.getBidderId(), request.getMaxAmount(),
                    null, null, null, null);
        }
    }
}
