package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.UserNotFoundException;
//import com.auction.server.factory.UserFromDTOFactory;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.UserDAO;
//import com.auction.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.SQLException;
import java.util.List;


public class UserService {
    private  UserDAO userDAO;

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_DISABLED = "DISABLED";

    public UserService() {
        this(new UserDAO());
    }

    public UserService(UserDAO userDAO) {
        if (userDAO == null) {
            throw new IllegalArgumentException("UserDAO cannot be null");
        }
        this.userDAO = userDAO;
    }

    private void requireActiveUser(User user) {
        if (user == null) {
            throw new UserNotFoundException("User not found");
        }

        if (!STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new AuctionException("Account is disabled");
        }
    }

// method public để các service khác gọi
    public User requireActiveUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        User user = userDAO.getUserById(userId);
        requireActiveUser(user);
        return user;
    }


    //=============== đăng ký ===============
    public void register(User user) {
        if (user == null) {
            throw new AuctionException("User must not be null!");
        }

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new AuctionException("Full name is required!");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new AuctionException("Username is required!");
        }

        if (user.getPassword() == null || user.getPassword().isBlank()) {
            throw new AuctionException("Password is required!");
        }

        User existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null) {
            throw new AuctionException("This username has already existed!");
        }

        //tạo id ngẫu nhiên
        String userId = java.util.UUID.randomUUID().toString();
        user.setId(userId);

        //băm password instead of plain text for security
        String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashed);

        userDAO.insertUser(user);
    }




    //=============== đăng nhập ===============
    public User login(String username, String password) {
        if (username == null || username.isBlank()) {
            throw new AuthenticationException("Username is required!");
        }

        if (password == null || password.isBlank()) {
            throw new AuthenticationException("Password is required!");
        }

        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            throw new AuthenticationException("Username does not exist!");
        }

        if (!STATUS_ACTIVE.equalsIgnoreCase(user.getStatus())) {
            throw new AuthenticationException("Account is disabled");
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new AuthenticationException("Incorrect password!");
        }
//        User user = UserFromDTOFactory.fromDTO(dto);
        //xóa mật khẩu để đảm bảo bảo mật trước khi trả ra ngoài
        user.setPassword(null);
        return user;
    }



// method không dùng vì không có tính năng này
    //=============== đổi mật khẩu ===============
    public void changePassword(String userId, String newPassword) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new AuctionException("New password is required!");
        }

        User user = userDAO.getUserById(userId);
        requireActiveUser(user);
        //băm password
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userDAO.updatePassword(userId, hashed);
    }




    //=============== cập nhật thông tin người dùng ===============
    public User updateProfile(String userId, String fullName, String username, String newPassword) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        if (fullName == null || fullName.isBlank()) {
            throw new AuctionException("Full name is required!");
        }

        if (username == null || username.isBlank()) {
            throw new AuctionException("Username is required!");
        }

        User current = userDAO.getUserById(userId);
        if (current == null) {
            throw new UserNotFoundException("User not found!");
        }

        requireActiveUser(current);

        User existing = userDAO.getUserByUsername(username);
        if (existing != null && !existing.getId().equals(userId)) {
            throw new AuctionException("This username has already existed!");
        }

        current.setFullName(fullName);
        current.setUsername(username);

        userDAO.updateUser(current);

        if (newPassword != null && !newPassword.isBlank()) {
            String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
            userDAO.updatePassword(userId, hashed);
        }

        return userDAO.getUserById(userId);
    }




    //=============== hiển thị toàn bộ thông tin người dùng ===============
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }




    //=============== hiển thị người dùng (qua id) ===============
    public User getUserById(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        User user = userDAO.getUserById(userId);

        if (user == null) {
            throw new UserNotFoundException("ID does not exist");
        }
        return user;
    }




    //=============== hiển thị người dùng (qua username) ===============
    public User getUserByUsername(String username) {
        if (username == null || username.isBlank()) {
            throw new AuctionException("Username is required!");
        }

        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("Username does not exist!");
        }
        return user;
    }




    //=============== hiển thị số dư tài khoản ===============
    public double getBalance(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        return userDAO.getAvailableBalance(userId);
    }

    public double getAvailableBalance(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        return userDAO.getAvailableBalance(userId);
    }


    //=============== bật chế độ SELLER ===============
    public User enableSeller(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        User user = userDAO.getUserById(userId);
        if (user == null) {
            throw new UserNotFoundException("User not found!");
        }
        requireActiveUser(user);

        if (!(user instanceof Bidder bidder)) {
            throw new AuctionException("Only bidder accounts can enable seller mode.");
        }

        if (bidder.isSellerEnabled()) {
            throw new AuctionException("Seller mode is already enabled.");
        }

        return userDAO.enableSeller(userId);
    }

    public User deposit(String userId, double amount) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        if (amount <= 0) {
            throw new AuctionException("Amount must be greater than 0.");
        }

        User user = userDAO.getUserById(userId);
        requireActiveUser(user);

        userDAO.updateBalance(userId, amount);
        return userDAO.getUserById(userId);
    }

    public void deleteUser(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        User user = userDAO.getUserById(userId);
        if (user == null) {
            throw new UserNotFoundException("User not found!");
        }

        if (STATUS_DISABLED.equalsIgnoreCase(user.getStatus())) {
            throw new AuctionException("User is already disabled!");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                userDAO.deactivateUser(conn, userId);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }
        } catch (SQLException e) {
            throw new AuctionException("Deactivate user failed! " + e.getMessage());
        }

        //userDAO.deleteUser(userId);
    }

}
