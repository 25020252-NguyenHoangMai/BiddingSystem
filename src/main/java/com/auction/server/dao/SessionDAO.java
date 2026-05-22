package com.auction.server.dao;

import com.auction.dto.ItemDTO;
import com.auction.dto.SellerHistoryItemDTO;
import com.auction.dto.SessionHistoryItemDTO;
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

    private AuctionSession mapToSession(Connection conn, ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String itemId = rs.getString("itemId");

        ItemDTO itemDTO = itemDAO.getItemById(conn, itemId);

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


    private SellerHistoryItemDTO mapToSessionHistoryItemDTOForSeller(ResultSet rs) throws SQLException {
        SellerHistoryItemDTO dto = new SellerHistoryItemDTO();

        dto.setSessionId(rs.getString("sessionId"));
        dto.setProductName(rs.getString("productName"));
        dto.setProductType(rs.getString("productType"));
        dto.setTotalBidsReceived(rs.getInt("totalBidsReceived"));
        dto.setStartingPrice(rs.getDouble("startingPrice"));
        dto.setCurrentPrice(rs.getDouble("currentPrice"));
        Timestamp startTime = rs.getTimestamp("startTime");
        if (startTime != null) {
            dto.setStartTimeMillis(startTime.getTime());
        }
        Timestamp endTime = rs.getTimestamp("endTime");
        if (endTime != null) {
            dto.setEndTimeMillis(endTime.getTime());
        }
        dto.setStatus(rs.getString("status"));
        dto.setImagePath(rs.getString("imagePath"));

        return dto;
    }

    public void insertSession(Connection conn, AuctionSession session, Item item) {
        String sql = """
            INSERT INTO AuctionSession (id, itemId, currentPrice, currentWinnerId, startTime, endTime, status)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public AuctionSession getSessionById(Connection conn, String sessionId) {
        String sql = "SELECT * FROM AuctionSession WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapToSession(conn, rs);
            }
            return null;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting session by id: " + e.getMessage());
        }
    }

    public AuctionSession getSessionByIdForUpdate(Connection conn, String sessionId) {
        String sql = """
            SELECT *
            FROM AuctionSession WITH (UPDLOCK, ROWLOCK)
            WHERE id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);
            ResultSet rs = ps.executeQuery();

            if (rs.next()) {
                return mapToSession(conn, rs);
            }
            return null;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting session by id for update: " + e.getMessage());
        }
    }

    public List<AuctionSession> getSessionsByItemIdForUpdate(Connection conn, String itemId) {
        String sql = """
        SELECT *
        FROM AuctionSession WITH (UPDLOCK, ROWLOCK)
        WHERE itemId = ?
    """;

        List<AuctionSession> sessions = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapToSession(conn, rs));
                }
            }
            return sessions;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while locking sessions by itemId: " + e.getMessage());
        }
    }

    public boolean updateCurrentBid(Connection conn, String sessionId, double newPrice, String bidderId) {
        String sql = """
            UPDATE AuctionSession
            SET currentPrice = ?, currentWinnerId = ?
            WHERE id = ? AND currentPrice < ?
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public void updateStatus(Connection conn, String sessionId, String status) {
        String sql = "UPDATE AuctionSession SET status = ? WHERE id = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
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

    public void updateEndTime(Connection conn, String sessionId, LocalDateTime newEndTime) {

        String sql = """
        UPDATE AuctionSession
        SET endTime = ?
        WHERE id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

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

    public void updateSchedule(Connection conn,
                               String sessionId,
                               LocalDateTime newStartTime,
                               LocalDateTime newEndTime) {
        String sql = """
        UPDATE AuctionSession
        SET startTime = ?, endTime = ?
        WHERE id = ?
    """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setTimestamp(1, Timestamp.valueOf(newStartTime));
            ps.setTimestamp(2, Timestamp.valueOf(newEndTime));
            ps.setString(3, sessionId);

            int updated = ps.executeUpdate();

            if (updated == 0) {
                throw new AuctionException("Auction session not found to update schedule.");
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while updating auction schedule: " + e.getMessage());
        }
    }

    public boolean existsSessionByItemId(Connection conn, String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId must not be null or empty");
        }

        String sql = "SELECT TOP 1 1 FROM AuctionSession WHERE itemId = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while checking session existence by itemId: " + e.getMessage());
        }
    }

    public boolean existsActiveSessionByItemId(Connection conn, String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId must not be null or empty");
        }

        String sql = """
        SELECT TOP 1 1
        FROM AuctionSession
        WHERE itemId = ?
          AND status IN ('OPEN', 'RUNNING')
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while checking active session by itemId: " + e.getMessage());
        }
    }

    public List<AuctionSession> getSessionsByItemId(Connection conn, String itemId) {
        if (itemId == null || itemId.trim().isEmpty()) {
            throw new IllegalArgumentException("itemId must not be null or empty");
        }

        String sql = "SELECT * FROM AuctionSession WHERE itemId = ?";

        List<AuctionSession> sessions = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapToSession(conn, rs));
                }
            }

            return sessions;

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting sessions by itemId: " + e.getMessage());
        }
    }

    public List<AuctionSession> getAllSessions(Connection conn) {
        String sql = "SELECT * FROM AuctionSession";
        List<AuctionSession> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapToSession(conn, rs));
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting all sessions: " + e.getMessage());
        }
        return list;
    }

    public List<AuctionSession> getSessionsByStatus(Connection conn, String status) {
        String sql = "SELECT * FROM AuctionSession WHERE status = ?";
        List<AuctionSession> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, status);
            ResultSet rs = ps.executeQuery();

            while (rs.next()) {
                list.add(mapToSession(conn, rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting sessions by status: " + e.getMessage());
        }

        return list;
    }

    public List<AuctionSession> getActiveSessionsBySellerForUpdate (Connection conn, String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new IllegalArgumentException("sellerId must not be null or empty");
        }
        String sql = """
            
                SELECT s.*
            FROM AuctionSession s WITH (UPDLOCK, ROWLOCK)
            JOIN Item i ON i.id = s.itemId
            WHERE i.sellerId = ?
            AND s.status IN ('OPEN', 'RUNNING')
            """;
        List<AuctionSession> sessions = new ArrayList<>();
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);
            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    sessions.add(mapToSession(conn, rs));
                }
            }
            return sessions;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while locking active seller sessions: " + e.getMessage());
        }
    }



    public List<SellerHistoryItemDTO> getSessionHistoryBySeller(Connection conn, String sellerId) {
        String sql = """
        SELECT
            s.id AS sessionId,
            i.name AS productName,
            i.itemType AS productType,
            i.startingPrice AS startingPrice,
            s.currentPrice AS currentPrice,
            s.startTime AS startTime,
            s.endTime AS endTime,
            (SELECT COUNT(*) FROM BidTransaction bt WHERE bt.sessionId = s.id) AS totalBidsReceived,
            s.status AS status,
            i.imagePath AS imagePath
        FROM AuctionSession s
        JOIN Item i ON i.id = s.itemId
        JOIN Users seller ON seller.id = i.sellerId
        WHERE i.sellerId = ?
        ORDER BY s.startTime DESC
    """;

        List<SellerHistoryItemDTO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sellerId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToSessionHistoryItemDTOForSeller(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting seller session history: " + e.getMessage());
        }
    }
}
