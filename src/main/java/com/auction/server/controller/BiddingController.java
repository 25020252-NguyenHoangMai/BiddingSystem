package com.auction.server.controller;

import com.auction.dto.UserSessionDTO;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.request.PlaceBidRequest;
import com.auction.request.SetAutoBidRequest;
import com.auction.response.BidUpdateResponse;
import com.auction.response.PlaceBidResponse;
import com.auction.response.SetAutoBidResponse;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.*;

import java.time.ZoneId;
import java.util.List;

public class BiddingController {
    private final BiddingService biddingService;
    private final SessionService sessionService;
    private final SessionWatchRegistry sessionWatchRegistry;
    private final AutoBiddingService autoBiddingService;
    private final BidIncrementService bidIncrementService;
    private final UserService userService;

    public BiddingController(BiddingService biddingService, SessionService sessionService,
                             SessionWatchRegistry sessionWatchRegistry, AutoBiddingService autoBiddingService,
                             BidIncrementService bidIncrementService, UserService userService) {
        this.biddingService = biddingService;
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.sessionService = sessionService;
        this.autoBiddingService = autoBiddingService;
        this.bidIncrementService = bidIncrementService;
        this.userService = userService;
    }

    public PlaceBidResponse placeBid(PlaceBidRequest request) {
        try {
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new PlaceBidResponse(false, "SessionId is required!", null,
                        null, null, null, null,
                        null, null);
            }

            if (request.getBidderId() == null || request.getBidderId().isBlank()) {
                return new PlaceBidResponse(false, "BidderId is required!", null,
                        null, null, null, null,
                        null, null);
            }

            if (request.getAmount() <= 0) {
                return new PlaceBidResponse(false, "Bid amount must be positive!", null,
                        null, null, null, null,
                        null, null);
            }

            BidResult result = biddingService.placeBid(request.getSessionId(), request.getBidderId(),
                                                request.getAmount());

            BidResult finalResult = result;

            if (result.isSuccess()) {
                broadcastBidUpdate(result);

                List<BidResult> autoBidResults = autoBiddingService.processAutoBidsAfterBid(
                        result.getSessionId(),
                        request.getBidderId()
                );

                for (BidResult autoBidResult : autoBidResults) {
                    if (autoBidResult.isSuccess()) {
                        broadcastBidUpdate(autoBidResult);
                        finalResult = autoBidResult;
                    }
                }
            }

            UserSessionDTO updatedUser = null;

            if (result.isSuccess()) {
                try {
                    User user = userService.getUserById(request.getBidderId());
                    updatedUser = toUserSessionDTO(user);
                } catch (Exception e) {
                    System.out.println("Bid succeeded but failed to reload updated user: " + e.getMessage());
                }
            }

            return new PlaceBidResponse(
                            result.isSuccess(),
                            result.getMessage(),
                            finalResult.getSessionId(),
                            finalResult.getCurrentPrice(),
                            finalResult.getCurrentWinnerId(),
                            finalResult.getCurrentWinnerUsername(),
                            finalResult.getStatus(),
                            getMinimumNextBid(finalResult.getCurrentPrice(), finalResult.getStatus()),
                            updatedUser
                    );

            //return new PlaceBidResponse(result.isSuccess(), result.getMessage(), result.getSessionId(),
                    //result.getCurrentPrice(), result.getCurrentWinnerId(),
                    //result.getCurrentWinnerUsername(), result.getStatus());

        } catch (InsufficientBalanceException e) {
            return new PlaceBidResponse(false, e.getMessage(), request.getSessionId(), null,
                    null, null, null, null, null);
        } catch (InvalidBidException | IllegalArgumentException e) {
            return new PlaceBidResponse(false, e.getMessage(), request.getSessionId(), null,
                    null, null, null, null, null);
        } catch (Exception e) {
            System.out.println("=== PLACE BID ERROR ===");
            e.printStackTrace();
            return new PlaceBidResponse(false, "Bid failed: unexpected server error!", null,
                    null, null, null, null,
                    null, null);
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
                    endTimeMillis,
                    getMinimumNextBid(result.getCurrentPrice(), result.getStatus())
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

            BidResult finalResult = result;

            if (result.isSuccess()) {
                broadcastBidUpdate(result);

                List<BidResult> autoBidResults = autoBiddingService.processAutoBidsAfterBid(
                        result.getSessionId(),
                        request.getBidderId()
                );

                for (BidResult autoBidResult : autoBidResults) {
                    if (autoBidResult.isSuccess()) {
                        broadcastBidUpdate(autoBidResult);
                        finalResult = autoBidResult;
                    }
                }
            }

            return new SetAutoBidResponse(result.isSuccess(), result.getMessage(), finalResult.getSessionId(),
                        request.getBidderId(), request.getMaxAmount(), finalResult.getCurrentPrice(),
                        finalResult.getCurrentWinnerId(), finalResult.getCurrentWinnerUsername(),
                        finalResult.getStatus());

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

    private Double getMinimumNextBid(Double currentPrice, String status) {
        if (currentPrice == null) {
            return null;
        }

        if (SessionService.STATUS_FINISHED.equals(status)
                || SessionService.STATUS_PAID.equals(status)
                || SessionService.STATUS_CANCELED.equals(status)) {
            return null;
        }

        return bidIncrementService.getMinimumNextBid(currentPrice);
    }

    private UserSessionDTO toUserSessionDTO(User user) {
        UserSessionDTO dto = new UserSessionDTO();

        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());

        if (user instanceof Bidder bidder) {
            dto.setBalance(bidder.getBalance());
            dto.setReservedBalance(bidder.getReservedBalance());
            dto.setSellerEnabled(bidder.isSellerEnabled());
        }

        return dto;
    }
}
