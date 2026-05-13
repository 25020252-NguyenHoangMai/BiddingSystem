package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.AutoBid;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class AutoBidDAO {
    private AutoBid mapToAutoBid(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String sessionId = rs.getString("sessionId");
        String bidderId = rs.getString("bidderId");
        double maxBidAmount = rs.getDouble("maxBidAmount");
        boolean isActive = rs.getBoolean("isActive");
        Timestamp createdAtts = rs.getTimestamp("createdAt");
        Timestamp updatedAtts = rs.getTimestamp("updatedAt");
        LocalDateTime createdAt = createdAtts != null ? createdAtts.toLocalDateTime() : null;
        LocalDateTime updatedAt = updatedAtts != null ? updatedAtts.toLocalDateTime() : null;

        return new AutoBid(id, sessionId, bidderId, maxBidAmount, isActive, createdAt, updatedAt);
    }

    //insert khi activate auto-bidding lần đầu và update nếu id auto-bidding này đã tồn tại
    public void upsertAutoBid(Connection conn, String id, double maxBidAmount) {
        String sql = "MERGE INTO AutoBid AS target " +
                "USING (SELECT ? AS id) AS source " +
                "ON (target.id = source.id) " +
                "WHEN MATCHED THEN " +
                "    UPDATE SET maxBidAmount = ?, isActive = 1, updatedAt = GETDATE() " +
                "WHEN NOT MATCHED THEN " +
                "    INSERT (id, sessionId, bidderId, maxBidAmount, isActive, createdAt, updatedAt) " +
                "    VALUES (source.id, source.sessionId, source.bidderId, ?, 1, GETDATE(), GETDATE());";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.setDouble(2, maxBidAmount);
            ps.setDouble(3, maxBidAmount);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while upserting auto-bidding: " + e.getMessage());
        }
    }

    //tắt chế độ auto-bidding
    public void deactivateAutoBid(Connection conn, String id) {
        String sql = "UPDATE AutoBid SET isActive = 0 AND updatedAt = GETDATE() WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while deactivating auto-bidding: " + e.getMessage());
        }
    }

    //truy vấn danh sách các auto-bids đang hoạt động
    public List<AutoBid> getActiveAutoBidsBySession(Connection conn, String sessionId) {
        List<AutoBid> list = new ArrayList<>();
        String sql = "SELECT * FROM AutoBid WHERE session_id = ? AND isActive = 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToAutoBid(rs));
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting active auto-bids by session: " + e.getMessage());
        }
        return list;
    }

    //truy vấn một auto-bid cụ thể đang hoạt động
    public AutoBid getActiveAutoBid(Connection conn, String id) {
        String sql = "SELECT * FROM AutoBid WHERE id = ? AND isActive = 1";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToAutoBid(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting the active auto-bid: " + e.getMessage());
        }
        return null;
    }

    //đóng toàn bộ auto-bids khi phiên đấu giá kết thúc
    public void deactivateAutoBidsBySession(Connection conn, String sessionId) {
        String sql = "UPDATE AutoBid SET isActive = 0, updatedAt = GETDATE() WHERE sessionId = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ps.executeUpdate();
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while deactivating active auto-bids by session: " + e.getMessage());
        }
    }
}
