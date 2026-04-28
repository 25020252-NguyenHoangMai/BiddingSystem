package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.UserNotFoundException;
//import com.auction.server.factory.UserFromDTOFactory;
import com.auction.model.User;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.UserDAO;
//import com.auction.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;


public class UserService {
    private  UserDAO userDAO;

    public UserService() {
        this.userDAO = new UserDAO();
    }

    public UserService(UserDAO mockDAO) {
        this.userDAO = mockDAO;
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

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new AuthenticationException("Incorrect password!");
        }
//        User user = UserFromDTOFactory.fromDTO(dto);
        //xóa mật khẩu để đảm bảo bảo mật trước khi trả ra ngoài
        user.setPassword(null);
        return user;
    }




    //=============== đổi mật khẩu ===============
    public void changePassword(String userId, String newPassword) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        if (newPassword == null || newPassword.isBlank()) {
            throw new AuctionException("New password is required!");
        }

        //băm password
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userDAO.updatePassword(userId, hashed);
    }




    //=============== cập nhật thông tin người dùng ===============
    public void updateProfile(User user) {
        if (user == null) {
            throw new AuctionException("User must not be null!");
        }

        if (user.getId() == null || user.getId().isBlank()) {
            throw new AuctionException("User id is required!");
        }

        if (user.getFullName() == null || user.getFullName().isBlank()) {
            throw new AuctionException("Full name is required!");
        }

        if (user.getUsername() == null || user.getUsername().isBlank()) {
            throw new AuctionException("Username is required!");
        }

        User existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new AuctionException("This username has already existed!");
        }
        userDAO.updateUser(user);
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

        return userDAO.getBalance(userId);
    }


    //=============== bật chế độ SELLER ===============
    public User enableSeller(String userId) {
        if (userId == null || userId.isBlank()) {
            throw new AuctionException("User id is required!");
        }

        return userDAO.enableSeller(userId);
    }

}
