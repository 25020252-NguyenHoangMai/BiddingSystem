package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.*;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;

public class ItemDAO {
    public void addItem(Item item, User user) {

        String randomID = java.util.UUID.randomUUID().toString();
        item.setId(randomID);

        String sql = "INSERT INTO Item (id, name, description, itemType, sellerID, startingPrice, model, engineType, mileage, brand, artist) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, item.getId());
            ps.setString(2, item.getName());
            ps.setString(3, item.getDescription());
            ps.setString(4, item.getItemType());
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
                ps.setNull(9, java.sql.Types.NVARCHAR);
                ps.setString(10, electronics.getBrand());
                ps.setNull(11, java.sql.Types.NVARCHAR);
            } else if (item instanceof Art art) {
                ps.setNull(7, java.sql.Types.NVARCHAR);
                ps.setNull(8, java.sql.Types.NVARCHAR);
                ps.setNull(9, java.sql.Types.NVARCHAR);
                ps.setNull(10, java.sql.Types.NVARCHAR);
                ps.setString(11, art.getArtist());
            }
            int affectedRows = ps.executeUpdate();
            if (affectedRows == 0) {
                throw new AuctionException("Thêm sản phẩm đấu giá thất bại, không có dòng nào được tạo.");
            }
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi hệ thống khi thêm sản phẩm: " + e.getMessage());
        }
    }
}
