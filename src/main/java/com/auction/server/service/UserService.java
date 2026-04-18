package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.factory.UserFactory;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import com.auction.server.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;
import com.auction.request.LoginRequest;
import com.auction.response.LoginResponse;

import java.util.List;
import java.util.stream.Collectors;

public class UserService {
    private final UserDAO userDAO = new UserDAO();

    public void register(User user) {
        UserDTO existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null) {
            throw new AuctionException("Username đã tồn tại!");
        }

        //tạo id ngẫu nhiên
        String id = java.util.UUID.randomUUID().toString();
        user.setId(id);

        //băm password instead of plain text for security
        String hashed = BCrypt.hashpw(user.getPassword(), BCrypt.gensalt());
        user.setPassword(hashed);

        userDAO.register(user);
    }
    public User login(String username, String password) {
        UserDTO dto = userDAO.getUserByUsername(username);

        if (dto == null) {
            throw new AuthenticationException("Username không tồn tại!");
        }

        if (!BCrypt.checkpw(password, dto.password)) {
            throw new AuthenticationException("Sai mật khẩu!");
        }
        User user = UserFactory.createUser(dto);
        user.setPassword(null);
        return user;
    }

    public void changePassword(String id, String newPassword) {

        //băm password
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userDAO.updatePassword(id, hashed);
    }

    public void updateProfile(User user) {
        UserDTO existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null && !existing.id.equals(user.getId())) {
            throw new AuctionException("Username đã tồn tại!");
        }
        userDAO.updateProfile(user);
    }
    public List<User> getAllUsers() {

        List<UserDTO> dtoList = userDAO.getAllUsers();

        return dtoList.stream()
                .map(UserFactory::createUser)
                .peek(u -> u.setPassword(null)) // tránh lộ password
                .collect(Collectors.toList());
    }

    public double getBalance(String userId) {
        return userDAO.getBalance(userId);
    }

    public LoginResponse login(LoginRequest request) {
        try {
            User user = login(request.getUsername(), request.getPassword());

            return new LoginResponse(
                    true,
                    "Đăng nhập thành công!",
                    user
            );

        } catch (AuthenticationException e) {
            return new LoginResponse(
                    false,
                    e.getMessage(),
                    null
            );

        } catch (Exception e) {
            return new LoginResponse(
                    false,
                    "Có lỗi xảy ra khi đăng nhập!",
                    null
            );
        }
    }
}
