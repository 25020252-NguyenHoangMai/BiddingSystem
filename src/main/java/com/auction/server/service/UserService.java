package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import org.mindrot.jbcrypt.BCrypt;

public class UserService {
    private UserDAO userDAO = new UserDAO();

    public void register(User user) {
        if (userDAO.isUsernameExist(user.getUsername())) {
            throw new AuctionException("Username đã tồn tại!");
        }

        String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashed);

        userDAO.register(user);
    }
    public User login(String username, String password) {
        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            throw new AuthenticationException("Username không tồn tại!");
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new AuthenticationException("Sai mật khẩu!");
        }

        return user;
    }

    public void changePassword(String id, String newPassword) {
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userDAO.updatePassword(id, hashed);
    }

    public void updateProfile(User user) {
        User existing = null;
        try {
            existing = userDAO.getUserByUsername(user.getUsername());
        } catch (AuctionException e) {}

        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new AuctionException("Username đã tồn tại!");
        }

        userDAO.updateProfile(user);
    }
}
