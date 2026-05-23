package com.auction.server.controller;

import com.auction.dto.BidHistoryEntryDTO;
import com.auction.dto.SessionHistoryItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.model.AuctionSession;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.request.*;
import com.auction.response.*;
import com.auction.server.dao.BidDAO;
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
    private final BidHistoryService bidHistoryService;

    public BiddingController(BiddingService biddingService, SessionService sessionService,
                             SessionWatchRegistry sessionWatchRegistry, AutoBiddingService autoBiddingService,
                             BidIncrementService bidIncrementService, UserService userService,
                             BidHistoryService bidHistoryService) {
        this.biddingService = biddingService;
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.sessionService = sessionService;
        this.autoBiddingService = autoBiddingService;
        this.bidIncrementService = bidIncrementService;
        this.userService = userService;
        this.bidHistoryService = bidHistoryService;
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
                    getMinimumNextBid(result.getCurrentPrice(), result.getStatus()),
                    result.getBidderUsername(),
                    result.getBidAmount()
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

    public GetBidHistoryResponse getBidHistory(GetBidHistoryRequest request) {
        try {
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new GetBidHistoryResponse(false, "Session ID is required", List.of());
            }

            List<BidHistoryEntryDTO> history =
                    bidHistoryService.getBidHistory(request.getSessionId());

            return new GetBidHistoryResponse(true, "Get bid history successfully", history);

        } catch (Exception e) {
            e.printStackTrace();
            return new GetBidHistoryResponse(false, "Get bid history failed: " + e.getMessage(), List.of());
        }
    }

    public GetSessionHistoryResponse getSessionHistory(GetSessionHistoryRequest request) {
        try {
            if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
                return new GetSessionHistoryResponse(false, "User ID is required", List.of());
            }

            List<SessionHistoryItemDTO> sessions = bidHistoryService.getSessionHistory(request.getUserId());

            return new GetSessionHistoryResponse(
                    true,
                    "Get session history successfully",
                    sessions
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new GetSessionHistoryResponse(
                    false,
                    "Get session history failed: " + e.getMessage(),
                    List.of()
            );
        }
    }

    public DisableAutoBidResponse disableAutoBid(DisableAutoBidRequest request) {
        try {
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new DisableAutoBidResponse(false, "SessionId is required", null,
                        request.getBidderId(), true, null, null,
                        null, null);
            }

            if (request.getBidderId() == null || request.getBidderId().isBlank()) {
                return new DisableAutoBidResponse(false, "BidderId is required", request.getSessionId(),
                        null, true, null, null, null,
                        null);
            }

            BidResult result = autoBiddingService.disableAutoBid(request.getSessionId(), request.getBidderId());

            return new DisableAutoBidResponse(result.isSuccess(),
                    result.getMessage(),
                    result.getSessionId(),
                    request.getBidderId(),
                    !result.isSuccess(),
                    result.getCurrentPrice(),
                    result.getCurrentWinnerId(),
                    result.getCurrentWinnerUsername(),
                    result.getStatus());
        } catch (IllegalArgumentException e) {
            return new DisableAutoBidResponse(false, e.getMessage(), request.getSessionId(),
                    request.getBidderId(), true, null, null,
                    null, null);
        } catch (Exception e) {
            e.printStackTrace();
            return new DisableAutoBidResponse(false, "Disable auto bid failed: unexpected server error!",
                    request.getSessionId(), request.getBidderId(), true, null,
                    null, null, null);
        }
    }
}
