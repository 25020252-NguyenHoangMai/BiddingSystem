package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.BidTransaction;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class BidDAO {

    private BidTransaction mapToBid(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String sessionId = rs.getString("sessionId");
        String bidderId = rs.getString("bidderId");
        double bidAmount = rs.getDouble("bidAmount");

        return new BidTransaction(id, sessionId, bidderId, bidAmount);
    }

    public void insertBid(BidTransaction bid) {

        String sql = "INSERT INTO BidTransaction (id, sessionId, bidderId, bidAmount, bidTime) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, bid.getId());
            ps.setString(2, bid.getSessionId());
            ps.setString(3, bid.getBidderId());
            ps.setDouble(4, bid.getBidAmount());
            ps.setTimestamp(5, Timestamp.valueOf(bid.getBidTime()));

            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while placing bid: " + e.getMessage());
        }
    }

    public BidTransaction getHighestBid(String sessionId) {
        String sql = """
        SELECT TOP 1 id, sessionId, bidderId, bidAmount, bidTime
        FROM BidTransaction
        WHERE sessionId = ?
        ORDER BY bidAmount DESC
    """;
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);

            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapToBid(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting highest bid: " + e.getMessage());
        }
    }

    public List<BidTransaction> getBidsBySession(String sessionId) {
        String sql = """
        SELECT id, sessionId, bidderId, bidAmount, bidTime
        FROM BidTransaction
        WHERE sessionId = ?
        ORDER BY bidTime DESC
    """;

        List<BidTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapToBid(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting bids by session: " + e.getMessage());
        }
        return list;
    }

    public List<BidTransaction> getBidsByBidder(String sessionId, String bidderId) {
        String sql = """
        SELECT id, sessionId, bidderId, bidAmount, bidTime
        FROM BidTransaction
        WHERE sessionId = ? AND bidderId = ?
        ORDER BY bidTime DESC
    """;

        List<BidTransaction> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.setString(2, bidderId);

            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapToBid(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting bids by bidder: " + e.getMessage());
        }

        return list;
    }
}
