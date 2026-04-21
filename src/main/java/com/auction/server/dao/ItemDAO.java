package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.*;
import com.auction.dto.ItemDTO;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

public class ItemDAO {

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


    //=============== thêm sản phẩm ===============
    public void insertItem(Item item) {
        String sql = "INSERT INTO Item (id, name, description, itemType, sellerId, startingPrice, model, engineType, mileage, brand, artist) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
                throw new AuctionException("Loại sản phẩm không hợp lệ");
            }
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi hệ thống khi thêm sản phẩm: " + e.getMessage());
        }
    }


    //=============== xóa sản phẩm ===============
    public void deleteItem(String id) {
        String sql = "DELETE FROM Item WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new AuctionException("Không xóa được sản phẩm");
            }
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi khi xóa thông tin: " + e.getMessage());
        }
    }


    //=============== cập nhật thông tin sản phẩm ===============
    public void updateItem(Item item) {
        String sql = "UPDATE Item SET name = ?, description = ?, itemType = ?, startingPrice = ?, model = ?, engineType = ?, mileage = ?, brand = ?, artist = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

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
                throw new AuctionException("Loại sản phẩm không hợp lệ");
            }
            ps.setString(10, item.getId());
            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi khi cập nhật thông tin: " + e.getMessage());
        }
    }

    //=============== hiển thị toàn bộ danh sách sản phẩm ===============
    public List<ItemDTO> getAllItems() {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM Item";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapToDTO(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi lấy danh sách sản phẩm.");
        }
        return list;
    }


    //=============== tìm user theo id ===============
    public ItemDTO getItemById(String id) {
        String sql = "SELECT * FROM Item WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToDTO(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm sản phẩm theo ID: " + e.getMessage());
        }
        return null;
    }


    //=============== tìm item theo name ===============
    public List<ItemDTO> getItemByName(String name) {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM Item WHERE name = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, name); // ✔ đặt ở đây

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToDTO(rs));
                }
            }
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm sản phẩm theo tên.");
        }
        return list;
    }

    //=============== tìm item theo itemType ===============
    public List<ItemDTO> getItemByItemType(String itemType) {
        List<ItemDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM Item WHERE itemType = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, itemType);

            try (ResultSet rs = ps.executeQuery()) {
                while (rs.next()) {
                    list.add(mapToDTO(rs));
                }
            }

        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm sản phẩm theo danh mục");
        }

        return list;
    }
}

