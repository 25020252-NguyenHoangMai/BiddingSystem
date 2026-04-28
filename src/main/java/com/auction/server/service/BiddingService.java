package com.auction.server.service;

import com.auction.model.AuctionSession;
import com.auction.model.BidTransaction;
import com.auction.server.dao.BidDAO;

import java.time.LocalDateTime;
import java.util.UUID;

public class BiddingService { // Xử lí đặt giá
    private final AntiSnipingService antiSnipingService;
    private final SessionService sessionService;
    private final BidDAO bidDAO;

    public BiddingService(SessionService sessionService, BidDAO bidDAO, AntiSnipingService antiSnipingService) {
        if (sessionService == null) {
            throw new IllegalArgumentException("SessionService must not be null");
        }
        if (bidDAO == null) {
            throw new IllegalArgumentException("BidDAO must not be null");
        }
        if (antiSnipingService == null) {
            throw new IllegalArgumentException("AntiSnipingService must not be null");
        }

        this.sessionService = sessionService;
        this.bidDAO = bidDAO;
        this.antiSnipingService = antiSnipingService;
    }

    public BidResult placeBid(String sessionId, String bidderId, double bidAmount) {
        validateBidInput(sessionId, bidderId, bidAmount);

        AuctionSession session = sessionService.getSession(sessionId);

        if (session == null) {
            return new BidResult(false, "Auction session not found: " + sessionId, sessionId,
                    0.0, null, null);
        }
        sessionService.refreshSessionStatus(sessionId);
        session = sessionService.getSession(sessionId);
        if (session == null) {
            return new BidResult(false, "Auction session not found after refresh: " + sessionId, sessionId,
                    0.0, null, null);
        }
        if (!isSessionCurrentlyBiddable(session)) {
            return new BidResult(false, buildNotBiddableMessage(session), session.getId(),
                    session.getCurrentPrice(), session.getCurrentWinnerId(), session.getStatus());
        }

        if (bidAmount <= session.getCurrentPrice()) {
            return new BidResult(false,
            "Bid failed: bid amount must be greater than current price (" + session.getCurrentPrice() + ").",
                    session.getId(), session.getCurrentPrice(), session.getCurrentWinnerId(), session.getStatus());
        }

        boolean updated = sessionService.updateCurrentBid(sessionId, bidAmount, bidderId);

        if (!updated) {
            AuctionSession latestSession = sessionService.getSession(sessionId);

            return new BidResult(false,
                    "Bid failed: session is closed or another higher bid was placed.", sessionId,
                    latestSession != null ? latestSession.getCurrentPrice() : 0.0,
                    latestSession != null ? latestSession.getCurrentWinnerId() : null,
                    latestSession != null ? latestSession.getStatus() : null);
        }

        BidTransaction bidTransaction = new BidTransaction(UUID.randomUUID().toString(),
                sessionId, bidderId, bidAmount);
        bidDAO.insertBid(bidTransaction);

        AuctionSession updatedSession = sessionService.getSession(sessionId);
        if (updatedSession == null) {
            return new BidResult(true, "Bid placed successfully but failed to reload updated session",
                    sessionId, bidAmount, bidderId, null);
        }

        //AntiSniping check
        boolean extended = false;
        if (antiSnipingService.shouldExtend(updatedSession)) {
            sessionService.extendSession(sessionId, antiSnipingService.getExtendTime());
            extended = true;

            updatedSession = sessionService.getSession(sessionId); //reload sau khi extend
        }

        String successMessage = extended ? "Bid placed successfully. Auction time extended due to anti-sniping."
                : "Bid placed successfully";

        return new BidResult(
                true,
                successMessage,
                updatedSession.getId(),
                updatedSession.getCurrentPrice(),
                updatedSession.getCurrentWinnerId(),
                updatedSession.getStatus()
        );
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
