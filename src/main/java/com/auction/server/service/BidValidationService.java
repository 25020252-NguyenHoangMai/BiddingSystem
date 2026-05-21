package com.auction.server.service;

import com.auction.exception.InvalidBidException;
import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.User;

import java.time.LocalDateTime;

public class BidValidationService {
    private final UserService userService;
    private final BidIncrementService bidIncrementService;

    public BidValidationService(UserService userService, BidIncrementService bidIncrementService) {
        if (userService == null) {
            throw new IllegalArgumentException("UserService must not be null");
        }

        if (bidIncrementService == null) {
            throw new IllegalArgumentException("BidIncrementService must not be null");
        }

        this.userService = userService;
        this.bidIncrementService = bidIncrementService;
    }

    public void validateBidInput(String sessionId, String bidderId, double bidAmount) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session id must not be empty");
        }

        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("Bidder id must not be empty");
        }

        if (bidAmount <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive");
        }
    }

    public Bidder requireBidder(String bidderId) {
        try {
            User user = userService.requireActiveUserById(bidderId);

            if (!(user instanceof Bidder bidder)) {
                throw new IllegalArgumentException("Only bidder accounts can place bids.");
            }

            return bidder;
        } catch (UserNotFoundException e) {
            throw new IllegalArgumentException("Bidder not found: " + bidderId);
        }
    }

    public boolean isSessionCurrentlyBiddable(AuctionSession session) {
        LocalDateTime now = LocalDateTime.now();

        return SessionService.STATUS_RUNNING.equals(session.getStatus())
                && !now.isBefore(session.getStartTime())
                && now.isBefore(session.getEndTime());
    }

    public String buildNotBiddableMessage(AuctionSession session) {
        String status = session.getStatus();

        if (SessionService.STATUS_OPEN.equals(status)) {
            return "Bid failed: auction session has not started yet.";
        }

        if (SessionService.STATUS_FINISHED.equals(status)) {
            return "Bid failed: auction session has already finished.";
        }

        if (SessionService.STATUS_CANCELED.equals(status)) {
            return "Bid failed: auction session was canceled.";
        }

        if (SessionService.STATUS_PAID.equals(status)) {
            return "Bid failed: auction session is already paid and closed.";
        }

        return "Bid failed: auction session is not available for bidding.";
    }

    public void validateBidIncrement(AuctionSession session, double bidAmount) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }

        double currentPrice = session.getCurrentPrice();
        double minimumNextBid = bidIncrementService.getMinimumNextBid(currentPrice);

        if (bidAmount < minimumNextBid) {
            throw new InvalidBidException("Bid failed: minimum next bid is " + minimumNextBid);
        }
    }

    public void validateSellerCannotBidOwnAuction(AuctionSession session, String bidderId) {
        if (session.getItem() == null || session.getItem().getSellerId() == null) {
            return;
        }

        if (session.getItem().getSellerId().equals(bidderId)) {
            throw new InvalidBidException("Bid failed: seller cannot bid on their own auction.");
        }
    }
}
