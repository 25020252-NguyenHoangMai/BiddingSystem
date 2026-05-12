package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.model.AuctionSession;
import com.auction.model.BidTransaction;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.UUID;

public class BidTransactionExecutor {
    private final BidDAO bidDAO;
    private final SessionDAO sessionDAO;
    private final UserDAO userDAO;
    private final BidValidationService bidValidationService;
    private final BidReservationCalculator bidReservationCalculator;

    public BidTransactionExecutor(BidDAO bidDAO, SessionDAO sessionDAO, UserDAO userDAO,
                    BidValidationService bidValidationService, BidReservationCalculator bidReservationCalculator) {
        if (bidDAO == null) {
            throw new IllegalArgumentException("BidDAO must not be null");
        }
        if (sessionDAO == null) {
            throw new IllegalArgumentException("SessionDAO must not be null");
        }
        if (userDAO == null) {
            throw new IllegalArgumentException("UserDAO must not be null");
        }
        if (bidValidationService == null) {
            throw new IllegalArgumentException("BidValidationService must not be null");
        }
        if (bidReservationCalculator == null) {
            throw new IllegalArgumentException("BidReservationCalculator must not be null");
        }

        this.bidDAO = bidDAO;
        this.sessionDAO = sessionDAO;
        this.userDAO = userDAO;
        this.bidValidationService = bidValidationService;
        this.bidReservationCalculator = bidReservationCalculator;
    }

    public BidExecutionResult execute(String sessionId, String bidderId, double bidAmount) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false); // tắt autocommit để tự quản lí transaction

            try {
                BidExecutionResult result = executeInsideTransaction(
                        conn,
                        sessionId,
                        bidderId,
                        bidAmount
                );

                if (result.isSuccess()) {
                    conn.commit();
                } else {
                    conn.rollback();
                }

                return result;
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
    }

    private BidExecutionResult executeInsideTransaction(Connection conn, String sessionId, String bidderId,
                                                    double bidAmount) throws SQLException {
        AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionId); //lock session (không request khác sửa cùng lúc)

        if (session == null) {
            return new BidExecutionResult(
                    false,
                    "Auction session not found: " + sessionId,
                    sessionId,
                    0.0,
                    null,
                    null
            );
        }

        bidValidationService.validateSellerCannotBidOwnAuction(session, bidderId);

        if (!bidValidationService.isSessionCurrentlyBiddable(session)) {
            return new BidExecutionResult(
                    false,
                    bidValidationService.buildNotBiddableMessage(session),
                    session.getId(),
                    session.getCurrentPrice(),
                    session.getCurrentWinnerId(),
                    session.getStatus()
            );
        }

        bidValidationService.validateBidIncrement(session, bidAmount);

        double currentPrice = session.getCurrentPrice();
        String oldWinnerId = session.getCurrentWinnerId();

        double reserveChange = bidReservationCalculator.calculateReserveChange(
                oldWinnerId,
                bidderId,
                currentPrice,
                bidAmount
        );

        if (reserveChange <= 0) {
            return new BidExecutionResult(
                    false,
                    "Bid failed: bid amount must be higher than current price.",
                    sessionId,
                    currentPrice,
                    oldWinnerId,
                    session.getStatus()
            );
        }

        UserBalance bidderBalance = userDAO.getBalanceForUpdate(conn, bidderId); //lock balance của bidder

        if (reserveChange > bidderBalance.getAvailableBalance()) {
            throw new InsufficientBalanceException("Bid failed: Insufficient available balance.");
        }

        if (oldWinnerId != null && !oldWinnerId.isBlank() && !oldWinnerId.equals(bidderId)) {
            userDAO.updateReservedBalance(conn, oldWinnerId, -currentPrice);
        }

        userDAO.updateReservedBalance(conn, bidderId, reserveChange);

        boolean updated = sessionDAO.updateCurrentBid(conn, sessionId, bidAmount, bidderId); //update bid của session

        if (!updated) {
            return new BidExecutionResult(
                    false,
                    "Bid failed: session is closed or another higher bid was placed.",
                    sessionId,
                    currentPrice,
                    oldWinnerId,
                    session.getStatus()
            );
        }

        //insert bid transaction
        BidTransaction bidTransaction = new BidTransaction(
                UUID.randomUUID().toString(),
                sessionId,
                bidderId,
                bidAmount
        );

        bidDAO.insertBid(conn, bidTransaction);

        return new BidExecutionResult(
                true,
                "Bid placed successfully",
                sessionId,
                bidAmount,
                bidderId,
                session.getStatus()
        );
    }

}
