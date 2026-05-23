package com.auction.server.dao;

import com.auction.dto.SessionHistoryItemDTO;
import com.auction.exception.AuctionException;
import com.auction.model.BidTransaction;

import java.sql.*;
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

    private SessionHistoryItemDTO mapToSessionHistoryItemDTO(ResultSet rs) throws SQLException {
        SessionHistoryItemDTO dto = new SessionHistoryItemDTO();

        dto.setSessionId(rs.getString("sessionId"));
        dto.setProductName(rs.getString("productName"));
        dto.setProductType(rs.getString("productType"));
        dto.setSellerId(rs.getString("sellerId"));
        dto.setSellerUsername(rs.getString("sellerUsername"));
        dto.setUserLastBid(rs.getDouble("userLastBid"));
        dto.setCurrentPrice(rs.getDouble("currentPrice"));

        Timestamp lastBidTime = rs.getTimestamp("lastBidTime");
        dto.setLastBidTime(lastBidTime != null ? lastBidTime.toLocalDateTime() : null);

        dto.setStatus(rs.getString("status"));
        dto.setImagePath(rs.getString("imagePath"));

        return dto;
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

    public List<SessionHistoryItemDTO> getSessionHistoryByBidder(Connection conn, String bidderId) {
        String sql = """
        -- Tạo một bảng tạm thời tên là RankedUserBids để gom nhóm và xếp hạng các lượt đặt bid
        WITH RankedUserBids AS (
            SELECT
                bt.id,
                bt.sessionId,
                bt.bidderId,
                bt.bidAmount,
                bt.bidTime,
                -- Sử dụng hàm cửa sổ ROW_NUMBER để đánh số thứ tự 1, 2, 3,... cho từng lượt đặt bid
                ROW_NUMBER() OVER (
                    PARTITION BY bt.sessionId
                    ORDER BY bt.bidTime DESC, bt.bidAmount DESC, bt.id DESC
                ) AS rn -- Đặt tên cột thứ tự này là rn
            FROM BidTransaction bt -- Lấy dữ liệu từ bảng BidTransaction
            WHERE bt.bidderId = ?
        )
        SELECT
            s.id AS sessionId,
            i.name AS productName,
            i.itemType AS productType,
            i.sellerId AS sellerId,
            seller.username AS sellerUsername,
            seller.fullName AS sellerFullName,
            r.bidAmount AS userLastBid,
            s.currentPrice AS currentPrice,
            r.bidTime AS lastBidTime,
            CASE
                WHEN s.status = 'CANCELED' THEN 'CANCELED'
                WHEN s.status IN ('FINISHED', 'PAID') AND s.currentWinnerId = ? THEN 'WON'
                WHEN s.status IN ('FINISHED', 'PAID') THEN 'LOST'
                ELSE s.status
            END AS status,
            i.imagePath AS imagePath
        FROM RankedUserBids r
        JOIN AuctionSession s ON s.id = r.sessionId
        JOIN Item i ON i.id = s.itemId
        JOIN Users seller ON seller.id = i.sellerId
        WHERE r.rn = 1
        ORDER BY r.bidTime DESC
    """;

        List<SessionHistoryItemDTO> list = new ArrayList<>();

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, bidderId);
            ps.setString(2, bidderId);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToSessionHistoryItemDTO(rs));
                }
            }
            return list;
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting session history: " + e.getMessage());
        }
    }

    public boolean existsBidBySessionId(Connection conn, String sessionId) {
        String sql = "SELECT TOP 1 1 FROM BidTransaction WHERE sessionId = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, sessionId);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while checking session bids: " + e.getMessage());
        }
    }

    
}
