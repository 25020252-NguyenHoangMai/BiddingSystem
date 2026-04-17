package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;


import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

public class UserDAO {


    //kiểm tra trùng lặp username
    public void checkDuplicate() {}

    //đăng ký - thêm user
    public void register(User user) {
        //tự động tạo id ngẫu nhiên không trùng lặp cho mỗi user khi đăng kí
        String randomID = java.util.UUID.randomUUID().toString();
        user.setId(randomID);

        String sql = "INSERT INTO Users (id, username, password, fullName, role, balance, storeName) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
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
        }
        catch (SQLException e) {
            // Có thể check mã lỗi SQL để ném message chuẩn hơn (ví dụ trùng username)
            throw new AuctionException("Lỗi hệ thống khi đăng ký: " + e.getMessage());
        }
    }


    //hiển thị thông tin (có thể được hiển thị) của 1 user cụ thể cho user khác thấy (tìm theo ID)
    public void displayUserInfo() {}

    //hiển thị danh sách tất cả users và thông tin của họ để admin quản lí
    public void displayAllUsers() {}

    //xác thực user qua username và password
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
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
            throw new AuctionException("Lỗi kết nối cơ sở dữ liệu khi xác thực.");
        }

        // nếu chạy xuống đến đây tức là không tìm thấy user
        throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không chính xác!");
    }



    //thay đổi thông tin của user
    public void updateProfile(User user) {
        String sql = "UPDATE Users SET username = ?, password = ?, fullName = ?, role = ?, storeName = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
            ps.setString(3, user.getFullName());
            ps.setString(4, user.getRole());


            //nếu user là bidder thì ở vị trí store name ta gán giá trị null
            if (user instanceof Bidder bidder) {
                ps.setNull(5, java.sql.Types.NVARCHAR);
            }

            //nếu user là seller thì có thể thay đổi thêm store name
            else if (user instanceof Seller seller) {
                ps.setString(5, seller.getStoreName());
            }

            //admin store name - null
            else {
                ps.setNull(5, java.sql.Types.NVARCHAR);
            }
            ps.setString(6, user.getId());
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi khi xóa thông tin: " + e.getMessage());
        }
    }

    //xóa user
    public void deleteUser(User user) {
        String sql = "DELETE FROM Users WHERE username = ? AND password = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, user.getUsername());
            ps.setString(2, user.getPassword());
        }

        catch (SQLException e) {
            throw new AuctionException("Lỗi khi cập nhật thông tin: " + e.getMessage());
        }
    }
}
