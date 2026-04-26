package com.auction.server.service;

import com.auction.model.AuctionSession;
import java.time.LocalDateTime;

public class BiddingService { // Xử lí đặt giá
    private final SessionService sessionService;

    public BiddingService(SessionService sessionService) {
        if (sessionService == null) {
            throw new IllegalArgumentException("SessionService must not be null");
        }

        this.sessionService = sessionService;
    }

    public BidResult placeBid(String sessionId, String bidderId, double bidAmount) {
        validateBidInput(sessionId, bidderId, bidAmount);

        AuctionSession session = sessionService.getSession(sessionId);

        if (session == null) {
            return new BidResult(false, "Auction session not found: " + sessionId, sessionId,
                    0.0, null, null);
        }

        synchronized (session) { // tránh race condition giữa các bidder
            sessionService.refreshSessionStatus(sessionId);

            if (!isSessionCurrentlyBiddable(session)) {
                return new BidResult(false, buildNotBiddableMessage(session), session.getId(),
                        session.getCurrentPrice(), session.getCurrentWinnerId(), session.getStatus());
            }

            if (bidAmount <= session.getCurrentPrice()) {
                return new BidResult(false,
                "Bid failed: bid amount must be greater than current price (" + session.getCurrentPrice() + ").",
                        session.getId(), session.getCurrentPrice(), session.getCurrentWinnerId(), session.getStatus()); //controller sau này trả response
            }

            session.setCurrentPrice(bidAmount);
            session.setCurrentWinnerId(bidderId);

            return new BidResult(true, "Bid placed successfully", session.getId(),
                    session.getCurrentPrice(), session.getCurrentWinnerId(), session.getStatus());
        }
    }

    private void validateBidInput(String sessionId, String bidderId, double bidAmount) {
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


    private boolean isSessionCurrentlyBiddable(AuctionSession session) {
        LocalDateTime now = LocalDateTime.now();

        return SessionService.STATUS_RUNNING.equals(session.getStatus())
                && !now.isBefore(session.getStartTime())
                && now.isBefore(session.getEndTime());
    }

    private String buildNotBiddableMessage(AuctionSession session) {
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
}
