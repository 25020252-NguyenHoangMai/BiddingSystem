package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.model.*;
import org.mindrot.jbcrypt.BCrypt;



import java.sql.*;

public class UserDAO {


    //kiểm tra trùng lặp username
    public boolean isAlreadyExist(User user) {
        String sql = "SELECT 1 FROM Users WHERE username = ?";

        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getUsername());

            try (ResultSet rs = ps.executeQuery()) {
                return rs.next();
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi kiểm tra username: " + e.getMessage());
        }
    }

    //đăng ký - thêm user
    public void register(User user) {
        if (isAlreadyExist(user)) {
            throw new AuctionException("Username đã tồn tại!");
        }
        //tự động tạo id ngẫu nhiên không trùng lặp cho mỗi user khi đăng kí
        String randomID = java.util.UUID.randomUUID().toString();
        user.setId(randomID);

        //băm password để lưu thay vì plain text for security (password đã bị băm không thể đảo ngược)
        String hashedPassword = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());


        String sql = "INSERT INTO Users (id, username, password, fullName, role, balance, storeName) VALUES (?, ?, ?, ?, ?, ?, ?)";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, user.getId());
            ps.setString(2, user.getUsername());
            ps.setString(3, hashedPassword);
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
            ps.executeUpdate();
        }
        catch (SQLException e) {
            // Có thể check mã lỗi SQL để ném message chuẩn hơn (ví dụ trùng username)
            throw new AuctionException("Lỗi hệ thống khi đăng ký: " + e.getMessage());
        }
    }

    //xác thực user qua username và password
    public User authenticate(String username, String password) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);

            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String hashedPassword = rs.getString("password");

                    if (!BCrypt.checkpw(password, hashedPassword)) {
                        throw new AuthenticationException("Sai mật khẩu!");
                    }

                    String role = rs.getString("role");
                    String id = rs.getString("id");
                    String user = rs.getString("username");
                    String name = rs.getString("fullName");

                    if ("ADMIN".equalsIgnoreCase(role)) {
                        return new Admin(id, user, null, name);
                    } else if ("BIDDER".equalsIgnoreCase(role)) {
                        return new Bidder(id, user, null, name, rs.getDouble("balance"));
                    } else if ("SELLER".equalsIgnoreCase(role)) {
                        return new Seller(id, user, null, name, rs.getString("storeName"));
                    }
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi kết nối cơ sở dữ liệu khi xác thực.");
        }

        // nếu chạy xuống đến đây tức là không tìm thấy user
        throw new AuthenticationException("Tên đăng nhập hoặc mật khẩu không chính xác!");
    }

    /**
     * lấy thông tin user qua id
     * (1. Vinh danh người thắng cuộc;
     * 2. Khi người dùng đã đăng nhập và thực hiện các thao tác tiếp theo
     * Server chỉ cần dùng ID trong Session để lấy lại trạng thái mới nhất của User đó từ DB )
     */
    public User getUserById(String id) {
        String sql = "SELECT * FROM Users WHERE id = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String username = rs.getString("username");
                    String fullName = rs.getString("fullName");

                    if ("ADMIN".equalsIgnoreCase(role)) return new Admin(id, username, null, fullName);
                    if ("BIDDER".equalsIgnoreCase(role)) return new Bidder(id, username, null, fullName, rs.getDouble("balance"));
                    if ("SELLER".equalsIgnoreCase(role)) return new Seller(id, username, null, fullName, rs.getString("storeName"));
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm người dùng theo ID: " + e.getMessage());
        }
        throw new AuctionException("Không tìm thấy user");
    }
    public User getUserByUsername(String username) {
        String sql = "SELECT * FROM Users WHERE username = ?";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, username);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    String role = rs.getString("role");
                    String id = rs.getString("id");
                    String fullName = rs.getString("fullName");

                    if ("ADMIN".equalsIgnoreCase(role)) return new Admin(id, username, null, fullName);
                    if ("BIDDER".equalsIgnoreCase(role)) return new Bidder(id, username, null, fullName, rs.getDouble("balance"));
                    if ("SELLER".equalsIgnoreCase(role)) return new Seller(id, username, null, fullName, rs.getString("storeName"));
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi tìm người dùng theo username: " + e.getMessage());
        }
        throw new AuctionException("Không tìm thấy user");
    }


    /**
     * giúp hệ thống nhìn thấy toàn bộ người dùng trong database
     * để Hưng làm giao diện cho Admin
     *(xem ds tất cả bidder và seller
     * ;xem ai đang hoạt động, số dư của họ là bn
     * ;cho phép Admin xóa user)
     *
     */
    public java.util.List<User> getAllUsers() {
        java.util.List<User> userList = new java.util.ArrayList<>();
        String sql = "SELECT * FROM Users";
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                String role = rs.getString("role");
                String id = rs.getString("id");
                String username = rs.getString("username");
                String fullName = rs.getString("fullName");

                if ("ADMIN".equalsIgnoreCase(role)) {
                    userList.add(new Admin(id, username, null, fullName));
                } else if ("BIDDER".equalsIgnoreCase(role)) {
                    userList.add(new Bidder(id, username, null, fullName, rs.getDouble("balance")));
                } else if ("SELLER".equalsIgnoreCase(role)) {
                    userList.add(new Seller(id, username, null, fullName, rs.getString("storeName")));
                }
            }
        } catch (SQLException e) {
            throw new AuctionException("Lỗi khi lấy danh sách người dùng.");
        }
        return userList;
    }

    //kiểm tra số dư tài khoản trước khi đặt bid
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



    //thay đổi thông tin của user
    public void updateProfile(User user) {
        User existing = null;
        try {
            existing = getUserByUsername(user.getUsername());
        } catch (AuctionException e) {
            //không tìm thấy --> OK
        }

        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new AuctionException("Username đã tồn tại!");
        }


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

    //dành cho chức năng đổi mật khẩu hoặc quên mật khẩu
    public void updatePassword(String id, String newPassword) {
        String sql = "UPDATE Users SET password = ? WHERE id = ?";

        //băm password để lưu thay vì plain text for security (password đã bị băm không thể đảo ngược)
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        try (Connection conn = DatabaseManager.getInstance().getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, hashed);
            ps.setString(2, id);

            int rowsAffected = ps.executeUpdate();
            if (rowsAffected == 0) {
                throw new AuctionException("Không tìm thấy người dùng để cập nhật mật khẩu.");
            }

            System.out.println("Cập nhật mật khẩu thành công cho ID: " + id);
        } catch (SQLException e) {
            throw new AuctionException("Lỗi hệ thống khi cập nhật mật khẩu: " + e.getMessage());
        }
    }

    //cập nhật balance
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

    //xóa user
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
