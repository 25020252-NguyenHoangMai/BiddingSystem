package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.*;
import com.auction.dto.ItemDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

    //chuyển đổi dữ liệu từ dòng kết quả SQL (ResultSet) thành đối tượng DTO để truyền tải an toàn và hiển thị lên giao diện
    private ItemDTO mapToDTO(ResultSet rs) throws SQLException {
        ItemDTO data = new ItemDTO();
        data.setId(rs.getString("id"));
        data.setName(rs.getString("name"));
        data.setDescription(rs.getString("description"));
        data.setItemType(rs.getString("itemType"));
        data.setSellerId(rs.getString("sellerId"));
        data.setStartingPrice(rs.getDouble("startingPrice"));
        data.setModel(rs.getString("model"));
        data.setEngineType(rs.getString("engineType"));
        data.setMileage(rs.getInt("mileage"));
        data.setBrand(rs.getString("brand"));
        data.setArtist(rs.getString("artist"));
        return data;
    }

    private ItemDTO mapToDashboardDTO(ResultSet rs) throws SQLException {
        ItemDTO dto = mapToDTO(rs);

        dto.setSellerUsername(rs.getString("sellerUsername"));
        dto.setSessionId(rs.getString("sessionId"));
        dto.setCurrentPrice(rs.getDouble("currentPrice"));
        dto.setSessionStatus(rs.getString("sessionStatus"));

        Timestamp startTime = rs.getTimestamp("startTime");
        if (startTime != null) {
            dto.setStartTimeMillis(startTime.getTime());
        }

        Timestamp endTime = rs.getTimestamp("endTime");
        if (endTime != null) {
            dto.setEndTimeMillis(endTime.getTime());
        }

        dto.setCurrentWinnerUsername(rs.getString("currentWinnerUsername"));

        return dto;
    }


    //=============== thêm sản phẩm ===============
    public void insertItem(Connection conn, Item item) {
        String sql = "INSERT INTO Item (id, name, description, itemType, sellerId, startingPrice, model, engineType, mileage, brand, artist) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getClass().getSimpleName().toUpperCase());
            ps.setString(5, item.getSellerId());
            ps.setDouble(6, item.getStartingPrice());

            if (item instanceof Vehicle vehicle) {
                ps.setString(7, vehicle.getModel());
                ps.setString(8, vehicle.getEngineType());
                ps.setInt(9, vehicle.getMileage());
                ps.setNull(10, java.sql.Types.NVARCHAR);
                ps.setNull(11, java.sql.Types.NVARCHAR);
            } else if (item instanceof Electronics electronics) {
                ps.setNull(7, java.sql.Types.NVARCHAR);
                ps.setNull(8, java.sql.Types.NVARCHAR);
                ps.setNull(9, java.sql.Types.INTEGER);
                ps.setString(10, electronics.getBrand());
                ps.setNull(11, java.sql.Types.NVARCHAR);
            } else if (item instanceof Art art) {
                ps.setNull(7, java.sql.Types.NVARCHAR);
                ps.setNull(8, java.sql.Types.NVARCHAR);
                ps.setNull(9, java.sql.Types.INTEGER);
                ps.setNull(10, java.sql.Types.NVARCHAR);
                ps.setString(11, art.getArtist());
            }
            else {
                throw new AuctionException("Invalid item type.");
            }
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while inserting item: " + e.getMessage());
        }
    }


    //=============== xóa sản phẩm ===============
    public void deleteItem(Connection conn, String id) {
        String sql = "DELETE FROM Item WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new AuctionException("The item cannot be deleted.");
            }
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while deleting item: " + e.getMessage());
        }
    }


    //=============== cập nhật thông tin sản phẩm ===============
    public void updateItem(Connection conn, Item item) {
        String sql = "UPDATE Item SET name = ?, description = ?, itemType = ?, startingPrice = ?, model = ?, engineType = ?, mileage = ?, brand = ?, artist = ? WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getName());
            ps.setString(2, item.getDescription());
            ps.setString(3, item.getClass().getSimpleName().toUpperCase());
            ps.setDouble(4, item.getStartingPrice());

            if (item instanceof Vehicle vehicle) {
                ps.setString(5, vehicle.getModel());
                ps.setString(6, vehicle.getEngineType());
                ps.setInt(7, vehicle.getMileage());
                ps.setNull(8, java.sql.Types.NVARCHAR);
                ps.setNull(9, java.sql.Types.NVARCHAR);

            }
            else if (item instanceof Electronics electronic) {
                ps.setNull(5, java.sql.Types.NVARCHAR);
                ps.setNull(6, java.sql.Types.NVARCHAR);
                ps.setNull(7, java.sql.Types.INTEGER);
                ps.setString(8, electronic.getBrand());
                ps.setNull(9, java.sql.Types.NVARCHAR);
            }

            else if (item instanceof Art art) {
                ps.setNull(5, java.sql.Types.NVARCHAR);
                ps.setNull(6, java.sql.Types.NVARCHAR);
                ps.setNull(7, java.sql.Types.INTEGER);
                ps.setNull(8, java.sql.Types.NVARCHAR);
                ps.setString(9, art.getArtist());
            }
            else {
                throw new AuctionException("Inavlid item type.");
            }
            ps.setString(10, item.getId());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while updating item information: " + e.getMessage());
        }
    }

    //=============== hiển thị toàn bộ danh sách sản phẩm ===============
    public List<ItemDTO> getAllItems(Connection conn) {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM Item";
        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapToDTO(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting all items: " + e.getMessage());
        }
        return list;
    }


    //=============== tìm user theo id ===============
    public ItemDTO getItemById(Connection conn, String id) {
        String sql = "SELECT * FROM Item WHERE id = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToDTO(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting item by id: " + e.getMessage());
        }
        return null;
    }


    //=============== tìm item theo name ===============
    public List<ItemDTO> getItemByName(Connection conn, String name) {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM Item WHERE name = ?";
        try (PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); // ✔ đặt ở đây

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToDTO(rs));
                }
            }
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while getting items by name: " + e.getMessage());
        }
        return list;
    }

    //=============== tìm item theo itemType ===============
    public List<ItemDTO> getItemByItemType(Connection conn, String itemType) {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM Item WHERE itemType = ?";

        try (PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemType);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToDTO(rs));
                }
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting items by item type: " + e.getMessage());
        }

        return list;
    }

    public List<ItemDTO> getAllItemsForDashboard(Connection conn) {
        List<ItemDTO> list = new ArrayList<>();

        String sql = """
        SELECT 
            i.*,
            seller.username AS sellerUsername,
            s.id AS sessionId,
            s.currentPrice,
            s.status AS sessionStatus,
            s.startTime,
            s.endTime,
            winner.username AS currentWinnerUsername
        FROM Item i
        LEFT JOIN Users seller ON i.sellerId = seller.id
        OUTER APPLY (
            SELECT TOP 1 *
            FROM AuctionSession s
            WHERE s.itemId = i.id AND s.status IN ('OPEN', 'RUNNING')
            ORDER BY s.startTime DESC
        ) s
        LEFT JOIN Users winner ON s.currentWinnerId = winner.id
        WHERE s.id IS NOT NULL
        ORDER BY s.startTime DESC
        """;

        try (PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                ItemDTO dto = mapToDTO(rs);

                dto.setSellerUsername(rs.getString("sellerUsername"));
                dto.setSessionId(rs.getString("sessionId"));
                dto.setCurrentPrice(rs.getDouble("currentPrice"));
                dto.setSessionStatus(rs.getString("sessionStatus"));
                dto.setCurrentWinnerUsername(rs.getString("currentWinnerUsername"));

                Timestamp endTime = rs.getTimestamp("endTime");
                if (endTime != null) {
                    dto.setEndTimeMillis(endTime.getTime());
                }

                list.add(mapToDashboardDTO(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting dashboard items: " + e.getMessage());
        }

        return list;
    }
}

