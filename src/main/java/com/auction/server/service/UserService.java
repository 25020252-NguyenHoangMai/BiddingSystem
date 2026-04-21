package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.UserNotFoundException;
//import com.auction.server.factory.UserFromDTOFactory;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
//import com.auction.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;

import java.util.List;


public class UserService {
    private final UserDAO userDAO = new UserDAO();



    //=============== đăng ký ===============
    public void register(User user) {
        User existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null) {
            throw new AuctionException("Username đã tồn tại!");
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
        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            throw new AuthenticationException("Username không tồn tại!");
        }

        if (!BCrypt.checkpw(password, user.getPassword())) {
            throw new AuthenticationException("Sai mật khẩu!");
        }
//        User user = UserFromDTOFactory.fromDTO(dto);
        //xóa mật khẩu để đảm bảo bảo mật trước khi trả ra ngoài
        user.setPassword(null);
        return user;
    }




    //=============== đổi mật khẩu ===============
    public void changePassword(String userId, String newPassword) {

        //băm password
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userDAO.updatePassword(userId, hashed);
    }




    //=============== cập nhật thông tin người dùng ===============
    public void updateProfile(User user) {
        User existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null && !existing.getId().equals(user.getId())) {
            throw new AuctionException("Username đã tồn tại!");
        }
        userDAO.updateUser(user);
    }




    //=============== hiển thị toàn bộ thông tin người dùng ===============
    public List<User> getAllUsers() {
        return userDAO.getAllUsers();
    }




    //=============== hiển thị người dùng (qua id) ===============
    public User getUserById(String userId) {
        User user = userDAO.getUserById(userId);

        if (user == null) {
            throw new UserNotFoundException("ID không tồn tại!");
        }
        return user;
    }




    //=============== hiển thị người dùng (qua username) ===============
    public User getUserByUsername(String username) {
        User user = userDAO.getUserByUsername(username);

        if (user == null) {
            throw new UserNotFoundException("Username không tồn tại!");
        }
        return user;
    }




    //=============== hiển thị số dư tài khoản ===============
    public double getBalance(String userId) {
        return userDAO.getBalance(userId);
    }

}
