package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.UserNotFoundException;
import com.auction.server.factory.UserFactory;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import com.auction.server.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;


public class UserService {
    private final UserDAO userDAO = new UserDAO();



    //=============== đăng ký ===============
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

        userDAO.insertUser(user);
    }




    //=============== đăng nhập ===============
    public User login(String username, String password) {
        UserDTO dto = userDAO.getUserByUsername(username);

        if (dto == null) {
            throw new AuthenticationException("Username không tồn tại!");
        }

        if (!BCrypt.checkpw(password, dto.getPassword())) {
            throw new AuthenticationException("Sai mật khẩu!");
        }
        User user = UserFactory.createUser(dto);
        //xóa mật khẩu để đảm bảo bảo mật trước khi trả ra ngoài
        user.setPassword(null);
        return user;
    }




    //=============== đổi mật khẩu ===============
    public void changePassword(String id, String newPassword) {

        //băm password
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userDAO.updatePassword(id, hashed);
    }




    //=============== cập nhật thông tin người dùng ===============
    public void updateProfile(User user) {
        UserDTO existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new AuctionException("Username đã tồn tại!");
        }
        userDAO.updateUser(user);
    }




    //=============== hiển thị toàn bộ thông tin người dùng ===============
    public List<UserDTO> getAllUsers() {
        return userDAO.getAllUsers();
    }




    //=============== hiển thị người dùng (qua id) ===============
    public UserDTO getUserById(String id) {
        UserDTO dto = userDAO.getUserById(id);

        if (dto == null) {
            throw new UserNotFoundException("ID không tồn tại!");
        }
        return dto;
    }




    //=============== hiển thị người dùng (qua username) ===============
    public UserDTO getUserByUsername(String username) {
        UserDTO dto = userDAO.getUserByUsername(username);

        if (dto == null) {
            throw new UserNotFoundException("Username không tồn tại!");
        }
        return dto;
    }




    //=============== hiển thị số dư tài khoản ===============
    public double getBalance(String userId) {
        return userDAO.getBalance(userId);
    }

}
