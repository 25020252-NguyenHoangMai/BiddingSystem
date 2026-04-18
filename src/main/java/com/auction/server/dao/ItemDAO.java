package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemDAO {
    public void addItem(Item item, User user) {

        //set id random không trùng lặp cho mỗi sản phẩm
        String randomID = java.util.UUID.randomUUID().toString();
        item.setId(randomID);

        String sql = "INSERT INTO Item (id, name, description, itemType, sellerID, startingPrice, model, engineType, mileage, brand, artist) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getClass().getSimpleName().toUpperCase());
            ps.setString(5, user.getId());
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


    public void deleteItem(Item item) {
        String sql = "DELETE FROM Item WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getId());
            int rows = ps.executeUpdate();

            if (rows == 0) {
                throw new ItemNotFoundException("Sản phẩm không tồn tại");
            }
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi khi xóa thông tin: " + e.getMessage());
        }
    }



    public void updateItemInfo(Item item) {
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
            int rows = ps.executeUpdate();
            if (rows == 0) {
                throw new ItemNotFoundException("Sản phẩm không tồn tại");
            }
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi khi cập nhật thông tin: " + e.getMessage());
        }
    }
    public void getAllItems() {}
    public void getItemById() {}

}
