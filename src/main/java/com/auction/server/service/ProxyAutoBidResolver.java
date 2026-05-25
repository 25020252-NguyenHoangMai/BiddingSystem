package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.model.AuctionSession;
import com.auction.model.AutoBid;
import com.auction.model.BidTransaction;
import com.auction.server.dao.*;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

public class ProxyAutoBidResolver {
    private final AutoBidDAO autoBidDAO;
    private final SessionDAO sessionDAO;
    private final UserDAO userDAO;
    private final BidDAO bidDAO;
    private final BidIncrementService bidIncrementService;
    private final BidValidationService bidValidationService;
    private final BidReservationCalculator bidReservationCalculator;

    public ProxyAutoBidResolver(
            AutoBidDAO autoBidDAO,
            SessionDAO sessionDAO,
            UserDAO userDAO,
            BidDAO bidDAO,
            BidIncrementService bidIncrementService,
            BidValidationService bidValidationService,
            BidReservationCalculator bidReservationCalculator
    ) {
        this.autoBidDAO = autoBidDAO;
        this.sessionDAO = sessionDAO;
        this.userDAO = userDAO;
        this.bidDAO = bidDAO;
        this.bidIncrementService = bidIncrementService;
        this.bidValidationService = bidValidationService;
        this.bidReservationCalculator = bidReservationCalculator;
    }

    public BidExecutionResult resolve(String sessionId) {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                BidExecutionResult result = resolveInsideTransaction(conn, sessionId);

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

                throw new AuctionException("Proxy auto bid failed: " + e.getMessage());
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new AuctionException("Proxy auto bid database error: " + e.getMessage());
        }
    }

    private BidExecutionResult resolveInsideTransaction(Connection conn, String sessionId) throws SQLException {
        AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionId);

        if (session == null) {
            return new BidExecutionResult(false, "Auction session not found",
                    sessionId, 0.0, null, null);
        }

        if (!bidValidationService.isSessionCurrentlyBiddable(session)) {
            return new BidExecutionResult(false,
                    bidValidationService.buildNotBiddableMessage(session),
                    sessionId,
                    session.getCurrentPrice(),
                    session.getCurrentWinnerId(),
                    session.getStatus());
        }

        List<AutoBid> autoBids = autoBidDAO.getActiveAutoBidsBySessionSorted(conn, sessionId);

        if (autoBids.isEmpty()) {
            return new BidExecutionResult(false, "No active auto bids",
                    sessionId,
                    session.getCurrentPrice(),
                    session.getCurrentWinnerId(),
                    session.getStatus());
        }

        AutoBid highest = autoBids.get(0);
        AutoBid second = findSecondAutoBid(autoBids, highest.getBidderId());

        String oldWinnerId = session.getCurrentWinnerId();
        double oldPrice = session.getCurrentPrice();

        String newWinnerId = highest.getBidderId();

        double secondAutoMax = second == null ? 0.0 : second.getMaxBidAmount();
        double competitorAmount = secondAutoMax;

        if (oldWinnerId != null && !oldWinnerId.isBlank() && !oldWinnerId.equals(newWinnerId)) {
            competitorAmount = Math.max(competitorAmount, oldPrice);
        }

        double minimumWinningPrice = bidIncrementService.getMinimumNextBid(competitorAmount);
        double newPrice = Math.min(highest.getMaxBidAmount(), minimumWinningPrice);

        if (newPrice < oldPrice) {
            return new BidExecutionResult(false, "Auto bid did not improve current price",
                    sessionId, oldPrice, oldWinnerId, session.getStatus());
        }

        if (newWinnerId.equals(oldWinnerId) && newPrice <= oldPrice) {
            return new BidExecutionResult(false, "Auto bid did not change current state",
                    sessionId, oldPrice, oldWinnerId, session.getStatus());
        }

        if (!newWinnerId.equals(oldWinnerId) && newPrice <= oldPrice) {
            return new BidExecutionResult(false, "Auto bid max amount is not enough",
                    sessionId, oldPrice, oldWinnerId, session.getStatus());
        }

        double reserveChange = bidReservationCalculator.calculateReserveChange(
                oldWinnerId,
                newWinnerId,
                oldPrice,
                newPrice
        );

        UserBalance winnerBalance = userDAO.getBalanceForUpdate(conn, newWinnerId);

        if (reserveChange > winnerBalance.getAvailableBalance()) {
            throw new InsufficientBalanceException("Auto bid failed: insufficient available balance.");
        }

        if (oldWinnerId != null && !oldWinnerId.isBlank() && !oldWinnerId.equals(newWinnerId)) {
            userDAO.updateReservedBalance(conn, oldWinnerId, -oldPrice);
        }

        userDAO.updateReservedBalance(conn, newWinnerId, reserveChange);

        boolean updated = sessionDAO.updateCurrentBid(conn, sessionId, newPrice, newWinnerId);

        if (!updated) {
            return new BidExecutionResult(false,
                    "Auto bid failed: session was updated by another request.",
                    sessionId,
                    oldPrice,
                    oldWinnerId,
                    session.getStatus());
        }

        BidTransaction bid = new BidTransaction(
                UUID.randomUUID().toString(),
                sessionId,
                newWinnerId,
                newPrice
        );

        bidDAO.insertBid(conn, bid);

        long bidTimeMillis = bid.getBidTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli();

        return new BidExecutionResult(true,
                "Auto bid resolved successfully",
                sessionId,
                newPrice,
                newWinnerId,
                session.getStatus(),
                bidTimeMillis);
    }

    private AutoBid findSecondAutoBid(List<AutoBid> autoBids, String highestBidderId) {
        for (AutoBid autoBid : autoBids) {
            if (!autoBid.getBidderId().equals(highestBidderId)) {
                return autoBid;
            }
        }

        return null;
    }
}