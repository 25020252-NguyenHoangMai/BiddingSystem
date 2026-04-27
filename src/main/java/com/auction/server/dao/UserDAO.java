package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.*;
//import com.auction.dto.UserDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    //chuyển đổi dữ liệu từ dạng hàng và cột của SQL Server thành một đối tượng
    private User mapToUser(ResultSet rs) throws SQLException {
        String id = rs.getString("id");
        String username = rs.getString("username");
        String password = rs.getString("password");
        String fullName = rs.getString("fullName");
        String role = rs.getString("role");

        if ("ADMIN".equalsIgnoreCase(role)) {
            return new Admin(id, username, password, fullName);
        } else if ("BIDDER".equalsIgnoreCase(role)) {
            double balance = rs.getDouble("balance");
            boolean sellerEnabled = rs.getBoolean("sellerEnabled");

            Bidder bidder = new Bidder(id, username, password, fullName, "BIDDER", balance);
            bidder.setSellerEnabled(sellerEnabled);
            return bidder;
        }
        return null;
    }

    //=============== kiểm tra trùng lặp username ===============
    public boolean isUsernameExist(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";

        //sử dụng try-with-resources để đảm bảo tự động đóng Connection và PreparedStatement giúp tránh rò rỉ tài nguyên (memory leak) cho SQL Server
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             //chuẩn bị câu lệnh SQL: giúp ngăn chặn SQL Injection và tối ưu hiệu suất truy vấn
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while checking for username duplication: " + e.getMessage());
        }
    }

    //============== đăng ký - thêm user ==============
    public void insertUser(User user) {

        String sql = "INSERT INTO Users (id, username, password, fullName, role) VALUES (?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, user.getPassword());
            ps.setString(4, user.getFullName());
            ps.setString(5, user.getRole());

            ps.executeUpdate();
        }
        catch (SQLException e) {
            // Có thể check mã lỗi SQL để ném message chuẩn hơn (ví dụ trùng username)
            throw new AuctionException("An error occurred during registration: " + e.getMessage());
        }
    }


    //=============== bật chế độ SELLER ===============
    public User enableSeller(String userId) {
        String sql = "UPDATE Users SET sellerEnabled = 1 WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            ps.executeUpdate();

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while enabling selling: " + e.getMessage());
        }
        return null;
    }

    /**
     * lấy thông tin user qua id
     * (1. Vinh danh người thắng cuộc;
     * 2. Khi người dùng đã đăng nhập và thực hiện các thao tác tiếp theo
     * Server chỉ cần dùng ID trong Session để lấy lại trạng thái mới nhất của User đó từ DB )
     */
    public User getUserById(String userId) {
        String sql = "SELECT * FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting user by id: " + e.getMessage());
        }
        return null;
    }

    //=============== tìm user theo username ===============
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToUser(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting user by username: " + e.getMessage());
        }
        return null;
    }


    /**
     * giúp hệ thống nhìn thấy toàn bộ người dùng trong database
     * để Hưng làm giao diện cho Admin
     *(xem ds tất cả bidder và seller
     * ;xem ai đang hoạt động, số dư của họ là bn
     * ;cho phép Admin xóa user)
     *
     */
    public List<User> getAllUsers() {
        List<User> list = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapToUser(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting all users: " + e.getMessage());
        }
        return list;
    }

    //=============== kiểm tra số dư tài khoản trước khi đặt bid ===============
    public double getBalance(String userId) {
        String sql = "SELECT balance FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) return rs.getDouble("balance");
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting balance: " + e.getMessage());
        }
        throw new AuctionException("User is not found.");
    }



    //=============== thay đổi thông tin của user ===============
    public void updateUser(User user) {

        String sql = "UPDATE Users SET username = ?, fullName = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());
            ps.setString(3, user.getId());

            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while updating user information: " + e.getMessage());
        }
    }

    //=============== dành cho chức năng đổi mật khẩu hoặc quên mật khẩu ===============
    public void updatePassword(String userId, String newPassword) {
        String sql = "UPDATE Users SET password = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setString(2, userId);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new AuctionException("User is not found.");
            }
        }
        catch (SQLException e) {
            throw new AuctionException("An error occurred while updating password: " + e.getMessage());
        }
    }

    //=============== cập nhật balance ===============
    public void updateBalance(String userId, double amount) {
        //cập nhật số dư bằng cách cộng/trừ trực tiếp trong SQL
        String sql = "UPDATE Users SET balance = balance + ? WHERE id = ? AND (balance + ? >= 0)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            //amount - số tiền thay đổi (dương - cộng vào, âm - trừ đi)
            ps.setDouble(1, amount);
            ps.setString(2, userId);
            ps.setDouble(3, amount);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new AuctionException("User is not found.");
            }
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while updating balance: " + e.getMessage());
        }
    }

    //=============== xóa user ===============
    public void deleteUser(String userId) {
        String sql = "DELETE FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, userId);
            ps.executeUpdate();
        }

        catch (SQLException e) {
            throw new AuctionException("An error occurred while deleting user: " + e.getMessage());
        }
    }

}
