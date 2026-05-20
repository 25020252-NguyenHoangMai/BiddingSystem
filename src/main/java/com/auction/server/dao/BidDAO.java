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
        Timestamp ts = rs.getTimestamp("bidTime");

        BidTransaction bid = new BidTransaction(id, sessionId, bidderId, bidAmount);
        bid.setBidTime(ts != null ? ts.toLocalDateTime() : null);

        return bid;
    }

    public void insertBid(Connection conn, BidTransaction bid) {

        String sql = "INSERT INTO BidTransaction (id, sessionId, bidderId, bidAmount, bidTime) VALUES (?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

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

    public BidTransaction getHighestBid(Connection conn, String sessionId) {
        String sql = """
        SELECT TOP 1 id, sessionId, bidderId, bidAmount, bidTime
        FROM BidTransaction
        WHERE sessionId = ?
        ORDER BY bidAmount DESC, bidTime DESC;
    """;
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public List<BidTransaction> getBidsBySession(Connection conn, String sessionId) {
        String sql = """
        SELECT id, sessionId, bidderId, bidAmount, bidTime
        FROM BidTransaction
        WHERE sessionId = ?
        ORDER BY bidTime DESC
    """;

        List<BidTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public List<BidTransaction> getBidsByBidder(Connection conn, String bidderId) {
        String sql = """
        SELECT id, sessionId, bidderId, bidAmount, bidTime
        FROM BidTransaction
        WHERE bidderId = ?
        ORDER BY bidTime DESC
    """;

        List<BidTransaction> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidderId);

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
