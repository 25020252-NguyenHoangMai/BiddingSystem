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

    public boolean placeBid(String sessionId, String bidderId, double bidAmount) {
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("Bidder id must not be empty");
        }

        AuctionSession session = sessionService.getSession(sessionId);

        if (session == null) {
            throw new IllegalArgumentException("Auction session not found: " + sessionId);
        }

        synchronized (session) {
            sessionService.refreshSessionStatus(sessionId);

            if (!isSessionCurrentlyBiddable(session)) {
                throw new IllegalStateException("Auction session is not biddable");
            }

            if (bidAmount <= session.getCurrentPrice()) {
                return false;
            }

            session.setCurrentPrice(bidAmount);
            session.setCurrentWinnerId(bidderId);
            return true;
        }
    }

    private boolean isSessionCurrentlyBiddable(AuctionSession session) {
        LocalDateTime now = LocalDateTime.now();

        return SessionService.STATUS_RUNNING.equals(session.getStatus())
                && !now.isBefore(session.getStartTime())
                && now.isBefore(session.getEndTime());
    }
}
