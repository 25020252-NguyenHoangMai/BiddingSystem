package com.auction.server.service;

import com.auction.dto.BidHistoryEntryDTO;
import com.auction.dto.SessionHistoryItemDTO;
import com.auction.exception.AuctionException;
import com.auction.model.BidTransaction;
import com.auction.model.User;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.ZoneId;
import java.util.List;

public class BidHistoryService {
    private final BidDAO bidDAO;
    private final UserService userService;

    public BidHistoryService(BidDAO bidDAO, UserService userService) {
        if (bidDAO == null) {
            throw new IllegalArgumentException("BidDAO cannot be null");
        }
        if (userService == null) {
            throw new IllegalArgumentException("UserService cannot be null");
        }

        this.bidDAO = bidDAO;
        this.userService = userService;
    }

    public List<BidHistoryEntryDTO> getBidHistory(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Session ID is required");
        }

        List<BidTransaction> bids;

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            bids = bidDAO.getBidsBySession(conn, sessionId);
        } catch (SQLException e) {
            throw new AuctionException("Failed to load bid history: " + e.getMessage());
        }

        return bids.stream()
                .map(this::toHistoryEntry)
                .toList();
    }

    private BidHistoryEntryDTO toHistoryEntry(BidTransaction bid) {
        String username = resolveUsername(bid.getBidderId());

        long timeMillis = bid.getBidTime() != null
                ? bid.getBidTime()
                .atZone(ZoneId.systemDefault())
                .toInstant()
                .toEpochMilli()
                : 0L;

        return new BidHistoryEntryDTO(
                username,
                bid.getBidAmount(),
                timeMillis
        );
    }

    private String resolveUsername(String userId) {
        if (userId == null || userId.isBlank()) {
            return null;
        }

        try {
            User user = userService.getUserById(userId);
            return user.getUsername();
        } catch (Exception e) {
            return "Unknown";
        }
    }

    public List<SessionHistoryItemDTO> getSessionHistory(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new IllegalArgumentException("User ID is required");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return bidDAO.getSessionHistoryByBidder(conn, userId);
        } catch (SQLException e) {
            throw new AuctionException("Failed to load session history: " + e.getMessage());
        }
    }
}
