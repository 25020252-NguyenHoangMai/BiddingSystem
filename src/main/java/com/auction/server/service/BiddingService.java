package com.auction.server.service;

import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.User;

public class BiddingService { // Xử lí đặt giá
    private final AntiSnipingService antiSnipingService;
    private final SessionService sessionService;
    private final UserService userService;
    private final BidValidationService bidValidationService;
    private final BidTransactionExecutor bidTransactionExecutor;

    public BiddingService(SessionService sessionService, AntiSnipingService antiSnipingService, UserService userService,
                          BidValidationService bidValidationService, BidTransactionExecutor bidTransactionExecutor) {
        if (sessionService == null) {
            throw new IllegalArgumentException("SessionService must not be null");
        }
        if (antiSnipingService == null) {
            throw new IllegalArgumentException("AntiSnipingService must not be null");
        }
        if (userService == null) {
            throw new IllegalArgumentException("UserService must not be null");
        }
        if (bidValidationService == null) {
            throw new IllegalArgumentException("BidValidationService must not be null");
        }
        if (bidTransactionExecutor == null) {
            throw new IllegalArgumentException("BidTransactionExecutor must not be null");
        }

        this.sessionService = sessionService;
        this.antiSnipingService = antiSnipingService;
        this.userService = userService;
        this.bidValidationService = bidValidationService;
        this.bidTransactionExecutor = bidTransactionExecutor;
    }

    public BidResult placeBid(String sessionId, String bidderId, double bidAmount) {
        bidValidationService.validateBidInput(sessionId, bidderId, bidAmount);
        bidValidationService.requireBidder(bidderId);
        sessionService.refreshSessionStatus(sessionId);

        AuctionSession session = sessionService.getSession(sessionId);
        if (session == null) {
            throw new IllegalArgumentException("AuctionSession not found:" + sessionId);
        }

        if (bidderId.equals(session.getCurrentWinnerId())) {
            return new BidResult(false, "Current winner cannot bid again", session.getId(),
                    session.getCurrentPrice(), session.getCurrentWinnerId(),
                    resolveWinnerUsername(session.getCurrentWinnerId()), session.getStatus());
        }

        BidExecutionResult executionResult = bidTransactionExecutor.execute(sessionId, bidderId, bidAmount);

        if (!executionResult.isSuccess()) {
            return new BidResult(
                    false,
                    executionResult.getMessage(),
                    executionResult.getSessionId(),
                    executionResult.getCurrentPrice(),
                    executionResult.getWinnerId(),
                    resolveWinnerUsername(executionResult.getWinnerId()),
                    executionResult.getStatus()
            );
        }

        // sau transaction: reload session mới nhất
        AuctionSession updatedSession = sessionService.getSession(sessionId);

        String bidderUsername = resolveWinnerUsername(bidderId);

        if (updatedSession == null) {
            return new BidResult(true,"Bid placed successfully but failed to reload updated session",
                            sessionId, bidAmount, bidderId, bidderUsername,null, bidderUsername, bidAmount);
        }

        //antisniping check
        boolean extended = false;

        if (antiSnipingService.shouldExtend(updatedSession)) {
            sessionService.extendSession(sessionId, antiSnipingService.getExtendTime());
            extended = true;
            updatedSession = sessionService.getSession(sessionId);
        }

        String successMessage = extended
                ? "Bid placed successfully. Auction time extended due to anti-sniping."
                : "Bid placed successfully";

        return new BidResult(
                true,
                successMessage,
                updatedSession.getId(),
                updatedSession.getCurrentPrice(),
                updatedSession.getCurrentWinnerId(),
                resolveWinnerUsername(updatedSession.getCurrentWinnerId()),
                updatedSession.getStatus(),
                bidderUsername,
                bidAmount
        );
    }


    private String resolveWinnerUsername(String winnerId) {
        if (winnerId == null || winnerId.isBlank()) {
            return null;
        }

        try {
            User user = userService.getUserById(winnerId);
            return user.getUsername();
        } catch (UserNotFoundException e) {
            return null;
        }
    }

}
