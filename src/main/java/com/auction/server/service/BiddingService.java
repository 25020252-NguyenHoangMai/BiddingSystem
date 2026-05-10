package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.UUID;

public class BiddingService { // Xử lí đặt giá
    private final AntiSnipingService antiSnipingService;
    private final SessionService sessionService;
    private final UserService userService;
    private final BidIncrementService bidIncrementService;
    private final BidDAO bidDAO;
    private final SessionDAO sessionDAO;
    private final UserDAO userDAO;

    public BiddingService(SessionService sessionService, BidDAO bidDAO, AntiSnipingService antiSnipingService,
                            UserService userService, BidIncrementService bidIncrementService,
                            SessionDAO sessionDAO, UserDAO userDAO) {
        if (sessionService == null) {
            throw new IllegalArgumentException("SessionService must not be null");
        }
        if (bidDAO == null) {
            throw new IllegalArgumentException("BidDAO must not be null");
        }
        if (antiSnipingService == null) {
            throw new IllegalArgumentException("AntiSnipingService must not be null");
        }
        if (userService == null) {
            throw new IllegalArgumentException("UserService must not be null");
        }
        if (bidIncrementService == null) {
            throw new IllegalArgumentException("BidIncrementService must not be null");
        }
        if (sessionDAO == null) {
            throw new IllegalArgumentException("SessionDAO must not be null");
        }
        if (userDAO == null) {
            throw new IllegalArgumentException("UserDAO must not be null");
        }

        this.sessionService = sessionService;
        this.bidDAO = bidDAO;
        this.antiSnipingService = antiSnipingService;
        this.userService = userService;
        this.bidIncrementService = bidIncrementService;
        this.sessionDAO = sessionDAO;
        this.userDAO = userDAO;
    }

    public BidResult placeBid(String sessionId, String bidderId, double bidAmount) {
        validateBidInput(sessionId, bidderId, bidAmount);
        requireBidder(bidderId);
        sessionService.refreshSessionStatus(sessionId);

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); // tắt autocommit để tự quản lý transaction

            try {
                AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionId); //lock session (không request khác sửa cùng lúc)
                if (session == null) {
                    conn.rollback();
                    return new BidResult(false, "Auction session not found: " + sessionId,
                                        sessionId, 0.0,
                                        null, null, null);
                }

                validateSellerCannotBidOwnAuction(session, bidderId);

                if (!isSessionCurrentlyBiddable(session)) {
                    conn.rollback();
                    return new BidResult(false, buildNotBiddableMessage(session), session.getId(),
                                session.getCurrentPrice(), session.getCurrentWinnerId(),
                                resolveWinnerUsername(session.getCurrentWinnerId()), session.getStatus());

                }

                validateBidIncrement(session, bidAmount);

                double currentPrice = session.getCurrentPrice();
                String oldWinnerId = session.getCurrentWinnerId();

                double reserveChange;
                if (oldWinnerId != null && oldWinnerId.equals(bidderId)) {
                    reserveChange = bidAmount - currentPrice;
                } else {
                    reserveChange = bidAmount;
                }
                if (reserveChange <= 0) {
                    conn.rollback();
                    return new BidResult(false,
                                "Bid failed: bid amount must be higher than current price.",
                                        sessionId, currentPrice, oldWinnerId,
                                        resolveWinnerUsername(oldWinnerId), session.getStatus());
                }

                UserBalance bidderBalance = userDAO.getBalanceForUpdate(conn, bidderId); //lock balance của bidder

                if (reserveChange > bidderBalance.getAvailableBalance()) { //check available balance
                    throw new InsufficientBalanceException("Bid failed: Insufficient available balance.");
                }

                if (oldWinnerId != null && !oldWinnerId.isBlank() && !oldWinnerId.equals(bidderId)) {
                    userDAO.updateReservedBalance(conn, oldWinnerId, -currentPrice);
                }
                userDAO.updateReservedBalance(conn, bidderId, reserveChange);

                boolean updated = sessionDAO.updateCurrentBid(conn, sessionId, bidAmount, bidderId); //update bid của session
                if (!updated) {
                    conn.rollback();
                    return new BidResult(
                            false,
                            "Bid failed: session is closed or another higher bid was placed.",
                            sessionId, currentPrice, oldWinnerId,
                            resolveWinnerUsername(oldWinnerId), session.getStatus());
                }

                //insert bid transaction
                BidTransaction bidTransaction = new BidTransaction(UUID.randomUUID().toString(), sessionId, bidderId,
                                                bidAmount);
                bidDAO.insertBid(conn, bidTransaction);
                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                if (e instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw new AuctionException("Bid failed: transaction error: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (SQLException e) {
            throw new AuctionException("Bid failed: database error: " + e.getMessage());
        }

        // sau transaction: reload session mới nhất
        AuctionSession updatedSession = sessionService.getSession(sessionId);

        if (updatedSession == null) {
            return new BidResult(true,"Bid placed successfully but failed to reload updated session",
                            sessionId, bidAmount, bidderId, resolveWinnerUsername(bidderId),null);
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

    private Bidder requireBidder(String bidderId) {
        try {
            User user = userService.getUserById(bidderId);

            if (!(user instanceof Bidder bidder)) {
                throw new IllegalArgumentException("Only bidder accounts can place bids.");
            }

            return bidder;
        } catch (UserNotFoundException e) {
            throw new IllegalArgumentException("Bidder not found: " + bidderId);
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

// không dùng method này nữa (logic cũ)
//    private void validateBidderBalance(Bidder bidder, double bidAmount) {
//        if (bidder == null) {
//            throw new IllegalArgumentException("Bidder must not be null");
//        }
//
//        if (bidAmount > bidder.getBalance()) {
//            throw new InsufficientBalanceException("Bid failed: Insufficient balance.");
//        }
//    }

    private void validateBidIncrement(AuctionSession session, double bidAmount) {
        if (session == null) {
            throw new IllegalArgumentException("Session must not be null");
        }

        double currentPrice = session.getCurrentPrice();
        double minimumNextBid = bidIncrementService.getMinimumNextBid(currentPrice);

        if (bidAmount < minimumNextBid) {
            throw new InvalidBidException("Bid failed: minimum next bid is " + minimumNextBid);
        }
    }

    private void validateSellerCannotBidOwnAuction(AuctionSession session, String bidderId) {
        if (session.getItem() == null || session.getItem().getSellerId() == null) {
            return;
        }

        if (session.getItem().getSellerId().equals(bidderId)) {
            throw new InvalidBidException("Bid failed: seller cannot bid on their own auction.");
        }
    }
}
