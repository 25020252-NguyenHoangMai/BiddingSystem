package com.auction.server.dao;

import com.auction.dto.ItemDTO;
import com.auction.exception.AuctionException;
import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.server.factory.ItemFromDTOFactory;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class SessionDAO {

    private final ItemDAO itemDAO;

    public SessionDAO(ItemDAO itemDAO) {
        if (itemDAO == null) {
            throw new IllegalArgumentException("ItemDAO must not be null");
        }
        this.itemDAO = itemDAO;
    }

    private AuctionSession mapToSession(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("itemId");

        ItemDTO itemDTO = itemDAO.getItemById(itemId);

        if (itemDTO == null) {
            throw new SQLException("Item not found with id: " + itemId);
        }

        Item item = ItemFromDTOFactory.createItem(itemDTO);

        double currentPrice = rs.getDouble("currentPrice");
        String currentWinnerId = rs.getString("currentWinnerId");

        Timestamp startTs = rs.getTimestamp("startTime");
        Timestamp endTs = rs.getTimestamp("endTime");

        LocalDateTime startTime = startTs != null ? startTs.toLocalDateTime() : null;
        LocalDateTime endTime = endTs != null ? endTs.toLocalDateTime() : null;

        String status = rs.getString("status");

        AuctionSession session = new AuctionSession(id, item, startTime, endTime);
        session.setCurrentPrice(currentPrice);
        session.setCurrentWinnerId(currentWinnerId);
        session.setStatus(status);

        return session;
    }

    public void insertSession(AuctionSession session, Item item) {
        String sql = """
            INSERT INTO AuctionSession (id, itemId, currentPrice, currentWinnerId, startTime, endTime, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, session.getId());
            ps.setString(2, item.getId());
            ps.setDouble(3, session.getCurrentPrice());
            ps.setString(4, session.getCurrentWinnerId());
            ps.setTimestamp(5, Timestamp.valueOf(session.getStartTime()));
            ps.setTimestamp(6, Timestamp.valueOf(session.getEndTime()));
            ps.setString(7, session.getStatus());

            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while inserting session: " + e.getMessage());
        }
    }

    public AuctionSession getSessionById(String sessionId) {
        String sql = "SELECT * FROM AuctionSession WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapToSession(rs);
            }
            return null;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting session by id: " + e.getMessage());
        }
    }

    public boolean updateCurrentBid(String sessionId, double newPrice, String bidderId) {
        String sql = """
            UPDATE AuctionSession
            SET currentPrice = ?, currentWinnerId = ?
            WHERE id = ? AND currentPrice < ?
        """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setDouble(1, newPrice);
            ps.setString(2, bidderId);
            ps.setString(3, sessionId);
            ps.setDouble(4, newPrice);

            int rows = ps.executeUpdate();
            return rows > 0;

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while updating bid: " + e.getMessage());
        }
    }

    public void updateStatus(String sessionId, String status) {
        String sql = "UPDATE AuctionSession SET status = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ps.setString(2, sessionId);
            int updated = ps.executeUpdate();

            if (updated == 0) {
                throw new AuctionException("Auction session is not found to update status.");
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while updating status: " + e.getMessage());
        }
    }

    public void updateEndTime(String sessionId, LocalDateTime newEndTime) {

        String sql = """
        UPDATE AuctionSession
        SET endTime = ?
        WHERE id = ?
    """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(newEndTime));
            ps.setString(2, sessionId);

            int updated = ps.executeUpdate();

            if (updated == 0) {
                throw new AuctionException("Auction session is not found to update end time.");
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while updating end time: " + e.getMessage());
        }
    }

    public boolean existsSessionByItemId(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId must not be null or empty");
        }

        String sql = "SELECT TOP 1 1 FROM AuctionSession WHERE itemId = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while checking session existence by itemId: " + e.getMessage());
        }
    }

    public boolean existsActiveSessionByItemId(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId must not be null or empty");
        }

        String sql = """
        SELECT TOP 1 1
        FROM AuctionSession
        WHERE itemId = ?
          AND status IN ('OPEN', 'RUNNING')
        """;

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while checking active session by itemId: " + e.getMessage());
        }
    }

    public List<AuctionSession> getSessionsByItemId(String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId must not be null or empty");
        }

        String sql = "SELECT * FROM AuctionSession WHERE itemId = ?";

        List<AuctionSession> sessions = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapToSession(rs));
                }
            }

            return sessions;

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting sessions by itemId: " + e.getMessage());
        }
    }

    public List<AuctionSession> getAllSessions() {
        String sql = "SELECT * FROM AuctionSession";
        List<AuctionSession> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapToSession(rs));
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting all sessions: " + e.getMessage());
        }
        return list;
    }

    public List<AuctionSession> getSessionsByStatus(String status) {
        String sql = "SELECT * FROM AuctionSession WHERE status = ?";
        List<AuctionSession> list = new ArrayList<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapToSession(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting sessions by status: " + e.getMessage());
        }

        return list;
    }
}
