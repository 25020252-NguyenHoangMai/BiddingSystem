package com.auction.server.service;

import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.AutoBid;
import com.auction.model.User;
import com.auction.server.dao.AutoBidDAO;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class AutoBiddingService {
    private final AutoBidDAO autoBidDAO;
    private final BidValidationService bidValidationService;
    private final SessionService sessionService;
    private final BidIncrementService bidIncrementService;
    private final BidTransactionExecutor bidTransactionExecutor;
    private final UserService userService;

    public AutoBiddingService(BidValidationService bidValidationService, SessionService sessionService,
                              BidIncrementService bidIncrementService, BidTransactionExecutor bidTransactionExecutor,
                              UserService userService, AutoBidDAO autoBidDAO) {
        if (bidValidationService == null) {
            throw new IllegalArgumentException("BidValidationService must not be null");
        }
        if (sessionService == null) {
            throw new IllegalArgumentException("SessionService must not be null");
        }
        if (bidIncrementService == null) {
            throw new IllegalArgumentException("BidIncrementService must not be null");
        }
        if (bidTransactionExecutor == null) {
            throw new IllegalArgumentException("BidTransactionExecutor must not be null");
        }
        if (userService == null) {
            throw new IllegalArgumentException("UserService must not be null");
        }
        if (autoBidDAO == null) {
            throw new IllegalArgumentException("AutoBidDAO must not be null");
        }

        this.bidValidationService = bidValidationService;
        this.sessionService = sessionService;
        this.bidIncrementService = bidIncrementService;
        this.bidTransactionExecutor = bidTransactionExecutor;
        this.userService = userService;
        this.autoBidDAO = autoBidDAO;
    }

    public BidResult setAutoBid(String sessionId, String bidderId, double maxAmount) {
        validateAutoBidInput(sessionId, bidderId, maxAmount);
        bidValidationService.requireBidder(bidderId);
        sessionService.refreshSessionStatus(sessionId);

        AuctionSession session = sessionService.getSession(sessionId);
        if (session == null) {
            return new BidResult(false, "Auction session not found: " + sessionId, sessionId,
                    0.0, null, null, null);
        }

        bidValidationService.validateSellerCannotBidOwnAuction(session, bidderId);
        boolean success = bidValidationService.isSessionCurrentlyBiddable(session);
        if (!success) {
            return buildCurrentStateResult(false, bidValidationService.buildNotBiddableMessage(session),
                    session);
        }

        double minimumNextBid = bidIncrementService.getMinimumNextBid(session.getCurrentPrice());
        if (maxAmount < minimumNextBid) {
            return buildCurrentStateResult(false,
                    "Auto bid max amount must be at least " + minimumNextBid, session);
        }

        //Lưu vào map
        autoBidDAO.upsertAutoBid(sessionId, bidderId, maxAmount);

        AuctionSession latestSession = sessionService.getSession(sessionId);
        if (latestSession == null) {
            removeAutoBid(sessionId, bidderId); //nếu session vừa đc gọi lại bị null thì remove autobid vừa lưu
            return new BidResult(false, "Auction session not found: " + sessionId, sessionId,
                    0.0, null, null, null);
        }

        minimumNextBid = bidIncrementService.getMinimumNextBid(latestSession.getCurrentPrice());
        if (maxAmount < minimumNextBid) {
            removeAutoBid(sessionId, bidderId);
            return buildCurrentStateResult(false,
                    "Auto bid max amount must be at least " + minimumNextBid, latestSession);
        }

        if (bidderId.equals(latestSession.getCurrentWinnerId())) {
            return buildCurrentStateResult(true, "Auto bid enabled", latestSession);
        }

        return placeAutoBid(sessionId, bidderId);
    }

    private void validateAutoBidInput(String sessionId, String bidderId, double maxAmount) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("SessionId must not be empty");
        }
        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("BidderId must not be empty");
        }
        if (maxAmount <= 0) {
            throw new IllegalArgumentException("Max amount must be greater than 0");
        }
    }

    private BidResult buildCurrentStateResult(boolean success, String message, AuctionSession session) {
        return new BidResult(success, message, session.getId(), session.getCurrentPrice(),
                session.getCurrentWinnerId(), resolveWinnerUsername(session.getCurrentWinnerId()), session.getStatus());
    }

    private void removeAutoBid(String sessionId, String bidderId) {
        autoBidDAO.deactivateAutoBid(sessionId, bidderId);

        if (sessionAutoBids == null) {
            return;
        }

        sessionAutoBids.remove(bidderId);

        if (sessionAutoBids.isEmpty()) {
            autoBidsBySession.remove(sessionId);
        }
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

    private BidResult placeAutoBid(String sessionId, String bidderId) {
        AuctionSession session = sessionService.getSession(sessionId);

        if (session == null) {
            removeAutoBid(sessionId, bidderId);
            return new BidResult(false, "Auction session not found: " + sessionId,
                    sessionId, 0.0, null, null, null);
        }

        AutoBid autoBid = autoBidDAO.getActiveAutoBid(sessionId, bidderId);
        if (autoBid == null) {
            return buildCurrentStateResult(false, "Auto bid was not found", session);
        }

        double maxAmount = autoBid.getMaxBidAmount();
        double autoBidAmount = bidIncrementService.getMinimumNextBid(session.getCurrentPrice());

        if (maxAmount < autoBidAmount) {
            removeAutoBid(sessionId, bidderId);
            return buildCurrentStateResult(false, "Auto bid max amount is no longer enough", session);
        }

        BidExecutionResult executionResult = bidTransactionExecutor.execute(sessionId, bidderId, autoBidAmount);

        if (!executionResult.isSuccess()) {
            removeAutoBid(sessionId, bidderId);
            return new BidResult(false, executionResult.getMessage(), executionResult.getSessionId(),
                    executionResult.getCurrentPrice(), executionResult.getWinnerId(),
                    resolveWinnerUsername(executionResult.getWinnerId()), executionResult.getStatus());
        }

        AuctionSession updatedSession = sessionService.getSession(sessionId);
        if (updatedSession == null) {
            return new BidResult(true, "Auto bid placed but failed to reload session",
                    sessionId, autoBidAmount, bidderId, resolveWinnerUsername(bidderId), null);
        }

        return buildCurrentStateResult(true, "Auto bid placed successfully", updatedSession);
    }

    public List<BidResult> processAutoBidsAfterBid(String sessionId, String triggeringBidderId) {
        List<BidResult> results = new ArrayList<>();

        if (sessionId == null || sessionId.isBlank()) {
            return results;
        }

        int maxRounds = 50;
        int rounds = 0;

        while (rounds < maxRounds) {
            rounds++;

            sessionService.refreshSessionStatus(sessionId);
            AuctionSession session = sessionService.getSession(sessionId);
            if (session == null) {
                break;
            }
            if (!bidValidationService.isSessionCurrentlyBiddable(session)) {
                break;
            }

            String autoBidderId = findBestAutoBidder(session);
            if (autoBidderId == null) {
                break;
            }

            BidResult result = placeAutoBid(sessionId, autoBidderId);

            if (result.isSuccess()) {
                results.add(result);
                continue;
            }

            removeAutoBid(sessionId, autoBidderId);
        }

        return results;
    }

    private String findBestAutoBidder(AuctionSession session) {
        if (session == null || session.getId() == null || session.getId().isBlank()) {
            return null;
        }

        List<AutoBid> sessionAutoBids = autoBidDAO.getActiveAutoBidsBySession(session.getId());
        if (sessionAutoBids == null || sessionAutoBids.isEmpty()) {
            return null;
        }

        double minimumNextBid = bidIncrementService.getMinimumNextBid(session.getCurrentPrice());
        String currentWinnerId = session.getCurrentWinnerId();

        String bestBidderId = null;
        double bestMaxAmount = -1.0;

        for (AutoBid autoBid : sessionAutoBids) {
            if (autoBid == null || !autoBid.isActive()) {
                continue;
            }

            String bidderId = autoBid.getBidderId();
            double maxAmount = autoBid.getMaxBidAmount();

            if (bidderId == null || bidderId.isBlank()) {
                continue;
            }

            if (bidderId.equals(currentWinnerId)) {
                continue;
            }

            if (maxAmount < minimumNextBid) {
                continue;
            }

            if (bestBidderId == null || maxAmount > bestMaxAmount) {
                bestBidderId = bidderId;
                bestMaxAmount = maxAmount;
            }
        }

        return bestBidderId;
    }
}
