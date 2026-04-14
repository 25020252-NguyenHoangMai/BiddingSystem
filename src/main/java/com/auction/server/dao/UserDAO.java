package com.auction.server.dao;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            ps.setString(2, password);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String id = rs.getString("id");
                    String user = rs.getString("username");
                    String pass = rs.getString("password");
                    String name = rs.getString("fullName");


                    if ("ADMIN".equalsIgnoreCase(role)) {
                        return new Admin(id, user, pass, name);
                    } else if ("BIDDER".equalsIgnoreCase(role)) {
                        double balance = rs.getDouble("balance");
                        return new Bidder(id, user, pass, name, balance);
                    } else if ("SELLER".equalsIgnoreCase(role)) {
                        String storeName = rs.getString("storeName");
                        return new Seller(id, user, pass, name, storeName);
                    }
                }
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return null; // Đăng nhập thất bại
    }

    public boolean register(User user) {
        String sql = "INSERT INTO Users (id, username, password, fullName, role, balance, storeName) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole());

            if (user instanceof Bidder bidder) {
                ps.setDouble(6, bidder.getBalance());
                ps.setNull(7, java.sql.Types.NVARCHAR);
            } else if (user instanceof Seller seller) {
                ps.setDouble(6, 0.0);
                ps.setString(7, seller.getStoreName());
            } else {
                ps.setDouble(6, 0.0);
                ps.setNull(7, java.sql.Types.NVARCHAR);
            }

            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }

    //thay đổi thông tin của user
    public boolean updateProfile(User user) {
        String sql = "UPDATE Users SET username = ?, password = ?, fullName = ?, role = ?, storeName = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());
            ps.setString(6, user.getId());

            //nếu user là bidder thì ở vị trí store name ta gán giá trị null
            if (user instanceof Bidder bidder) {
                ps.setNull(5, java.sql.Types.NVARCHAR);
            }

            //nếu user là seller thì có thể thay đổi thêm store name
            else if (user instanceof Seller seller) {
                ps.setString(5, seller.getStoreName());
            }

            //admin tất cả đều được set mặc định và null
            else {
                ps.setDouble(6, 0.0);
                ps.setNull(7, java.sql.Types.NVARCHAR);
            }

            //lệnh executeUpdate() dùng để thực thi lệnh UPDATE và trả về số dòng được update
            int rowsUpdated = ps.executeUpdate();
            return rowsUpdated > 0;
        }
        catch (SQLException e) {
            e.printStackTrace();
            return false;
        }
    }
}
