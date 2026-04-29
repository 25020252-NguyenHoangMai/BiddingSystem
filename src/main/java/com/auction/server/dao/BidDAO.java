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

    public void insertBid(BidTransaction bid) {

        String sql = "INSERT INTO BidTransaction (id, sessionId, bidderId, bidAmount, bidTime) VALUES (?, ?, ?, ?, ?)";
//        String sql = """
//            INSERT INTO BidTransaction (id, sessionId, bidderId, bidAmount, bidTime)
//            VALUES (?, ?, ?, ?, SYSDATETIME())
//        """;
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
        ORDER BY bidAmount DESC, bidTime DESC;
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

//    public boolean placeBidAtomically(String sessionId, String bidderId, double bidAmount) {
//
//        String selectSessionForUpdate = """
//        SELECT currentPrice, currentWinnerId, status
//        FROM AuctionSession WITH (UPDLOCK, ROWLOCK)
//        WHERE id = ?
//    """;
//
//        String updateSession = """
//        UPDATE AuctionSession
//        SET currentPrice = ?, currentWinnerId = ?
//        WHERE id = ?
//    """;
//
////        String insertBid = """
////        INSERT INTO BidTransaction (id, sessionId, bidderId, bidAmount, bidTime)
////        VALUES (?, ?, ?, ?, ?)
////    """;
//
//        String insertBid = """
//            INSERT INTO BidTransaction (id, sessionId, bidderId, bidAmount, bidTime)
//            VALUES (?, ?, ?, ?, SYSDATETIME())
//        """;
//
//        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
//
//            conn.setAutoCommit(false);
//
//            try (
//                    PreparedStatement psSelect = conn.prepareStatement(selectSessionForUpdate);
//                    PreparedStatement psUpdate = conn.prepareStatement(updateSession);
//                    PreparedStatement psInsert = conn.prepareStatement(insertBid)
//            ) {
//
//                //lock row
//                psSelect.setString(1, sessionId);
//                ResultSet rs = psSelect.executeQuery();
//
//                if (!rs.next()) {
//                    throw new AuctionException("Auction session not found");
//                }
//
//                double currentPrice = rs.getDouble("currentPrice");
//                String currentWinnerId = rs.getString("currentWinnerId");
//                String status = rs.getString("status");
//
//                if (!"RUNNING".equalsIgnoreCase(status)) return false;
//                if (bidAmount <= currentPrice) return false;
//                if (bidderId.equals(currentWinnerId)) return false;
//
//                psUpdate.setDouble(1, bidAmount);
//                psUpdate.setString(2, bidderId);
//                psUpdate.setString(3, sessionId);
//
//                int updated = psUpdate.executeUpdate();
//                if (updated == 0) {
//                    throw new AuctionException("Failed to update session.");
//                }
//
//                BidTransaction bid = new BidTransaction(
//                        java.util.UUID.randomUUID().toString(),
//                        sessionId,
//                        bidderId,
//                        bidAmount
//                );
////                bid.setBidTime(LocalDateTime.now());
//
//                psInsert.setString(1, bid.getId());
//                psInsert.setString(2, bid.getSessionId());
//                psInsert.setString(3, bid.getBidderId());
//                psInsert.setDouble(4, bid.getBidAmount());
////                psInsert.setTimestamp(5, Timestamp.valueOf(bid.getBidTime()));
//
//                psInsert.executeUpdate();
//
//                conn.commit();
//                return true;
//
//            } catch (Exception e) {
//                conn.rollback();
//                throw e;
//            }
//
//        } catch (SQLException e) {
//            throw new AuctionException("Atomic bid placement failed: " + e.getMessage());
//        }
//    }
}
