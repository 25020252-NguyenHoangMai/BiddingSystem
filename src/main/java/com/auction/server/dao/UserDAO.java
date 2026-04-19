package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.*;
import com.auction.server.dto.UserDTO;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class UserDAO {

    private UserDTO mapToDTO(ResultSet rs) throws SQLException {
        UserDTO data = new UserDTO();
        data.setId(rs.getString("id"));
        data.setUsername(rs.getString("username"));
        data.setPassword(rs.getString("password"));
        data.setFullName(rs.getString("fullName"));
        data.setRole(rs.getString("role"));
        data.setBalance(rs.getDouble("balance"));
        data.setStoreName(rs.getString("storeName"));
        return data;
    }

    //=============== kiểm tra trùng lặp username ===============
    public boolean isUsernameExist(String username) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi kiểm tra username: " + e.getMessage());
        }
    }

    //============== đăng ký - thêm user ==============
    public void register(User user) {

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
                ps.setNull(7, Types.NVARCHAR);
            } else if (user instanceof Seller seller) {
                ps.setDouble(6, 0.0);
                ps.setString(7, seller.getStoreName());
            } else {
                ps.setDouble(6, 0.0);
                ps.setNull(7, Types.NVARCHAR);
            }
            ps.executeUpdate();
        }
        catch (SQLException e) {
            // Có thể check mã lỗi SQL để ném message chuẩn hơn (ví dụ trùng username)
            throw new AuctionException("Lỗi hệ thống khi đăng ký: " + e.getMessage());
        }
    }

    /**
     * lấy thông tin user qua id
     * (1. Vinh danh người thắng cuộc;
     * 2. Khi người dùng đã đăng nhập và thực hiện các thao tác tiếp theo
     * Server chỉ cần dùng ID trong Session để lấy lại trạng thái mới nhất của User đó từ DB )
     */
    public UserDTO getUserById(String id) {
        String sql = "SELECT * FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToDTO(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm người dùng theo ID: " + e.getMessage());
        }
        return null;
    }

    //=============== tìm user theo username ===============
    public UserDTO getUserByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToDTO(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm người dùng theo username: " + e.getMessage());
        }
        return null;
    }

    //=============== tìm user theo role ===============
    public UserDTO getUserByRole(String role) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, role);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapToDTO(rs);
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm người dùng theo username: " + e.getMessage());
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
    public List<UserDTO> getAllUsers() {
        List<UserDTO> list = new ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                list.add(mapToDTO(rs));
            }

        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi lấy danh sách người dùng.");
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
            throw new AuctionException("Lỗi hệ thống khi truy vấn số dư.");
        }
        throw new AuctionException("Không tìm thấy user để lấy số dư");
    }



    //=============== thay đổi thông tin của user ===============
    public void updateProfile(User user) {

        String sql = "UPDATE Users SET username = ?, fullName = ?, storeName = ? WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());
            ps.setString(2, user.getFullName());

            if (user instanceof Bidder bidder) {
                ps.setNull(3, java.sql.Types.NVARCHAR);
            }
            else if (user instanceof Seller seller) {
                ps.setString(3, seller.getStoreName());
            }
            else {
                ps.setNull(3, java.sql.Types.NVARCHAR);
            }
            ps.setString(4, user.getId());

            ps.executeUpdate();
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi khi cập nhật thông tin: " + e.getMessage());
        }
    }

    //=============== dành cho chức năng đổi mật khẩu hoặc quên mật khẩu ===============
    public void updatePassword(String id, String newPassword) {
        String sql = "UPDATE Users SET password = ? WHERE id = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, newPassword);
            ps.setString(2, id);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new AuctionException("Không tìm thấy người dùng để cập nhật mật khẩu.");
            }
        }
        catch (SQLException e) {
            throw new AuctionException("Lỗi hệ thống khi cập nhật mật khẩu: " + e.getMessage());
        }
    }

    //=============== cập nhật balance ===============
    public void updateBalance(String id, double amount) {
        //cập nhật số dư bằng cách cộng/trừ trực tiếp trong SQL
        String sql = "UPDATE Users SET balance = balance + ? WHERE id = ? AND (balance + ? >= 0)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            //amount - số tiền thay đổi (dương - cộng vào, âm - trừ đi)
            ps.setDouble(1, amount);
            ps.setString(2, id);
            ps.setDouble(3, amount);

            int rowsAffected = ps.executeUpdate();

            if (rowsAffected == 0) {
                throw new AuctionException("Không tìm thấy người dùng với ID hoặc số dư không đủ: " + id);
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi cập nhật số dư: " + e.getMessage());
        }
    }

    //=============== xóa user ===============
    public void deleteUser(String id) {
        String sql = "DELETE FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {
            ps.setString(1, id);
            ps.executeUpdate();
        }

        catch (SQLException e) {
            throw new AuctionException("Lỗi khi xóa người dùng: " + e.getMessage());
        }
    }

}
