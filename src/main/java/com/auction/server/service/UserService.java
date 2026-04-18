package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.server.factory.UserFactory;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import com.auction.server.dto.UserDTO;
import org.mindrot.jbcrypt.BCrypt;
import com.auction.request.LoginRequest;
import com.auction.response.LoginResponse;
import com.auction.request.RegisterRequest;
import com.auction.response.RegisterResponse;

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

        userDAO.register(user);
    }




    //=============== đăng nhập ===============
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




    //=============== đổi mật khẩu ===============
    public void changePassword(String id, String newPassword) {

        //băm password
        String hashed = BCrypt.hashpw(newPassword, BCrypt.gensalt());
        userDAO.updatePassword(id, hashed);
    }




    //=============== cập nhật thông tin người dùng ===============
    public void updateProfile(User user) {
        UserDTO existing = userDAO.getUserByUsername(user.getUsername());

        if (existing != null && !existing.id.equals(user.getId())) {
            throw new AuctionException("Username đã tồn tại!");
        }
        userDAO.updateProfile(user);
    }




    //=============== hiển thị toàn bộ thông tin người dùng ===============
    public List<UserDTO> getAllUsers() {
        return userDAO.getAllUsers();
    }




    //=============== hiển thị người dùng (qua id) ===============
//    public UserDTO getUserById(String id) {
//        UserDTO dto = userDAO.getUserById(id);
//
//        if (dto == null) {
//            throw new UserNotFoundException("ID không tồn tại!");
//        }
//        return dto;
//    }




    //=============== hiển thị người dùng (qua username) ===============
//    public UserDTO getUserByUsername(String username) {
//        UserDTO dto = userDAO.getUserByUsername(username);
//
//        if (dto == null) {
//            throw new UserNotFoundException("Username không tồn tại!");
//        }
//        return dto;
//    }




    //=============== hiển thị số dư tài khoản ===============
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
            e.printStackTrace();
            return new LoginResponse(
                    false,
                    "Có lỗi xảy ra khi đăng nhập!",
                    null
            );
        }
    }

    public RegisterResponse register(RegisterRequest request) {
        try {
            if (request.getFullName() == null || request.getFullName().isBlank()) {
                return new RegisterResponse(false, "Full name is required!");
            }

            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return new RegisterResponse(false, "Username is required!");
            }

            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return new RegisterResponse(false, "Password is required!");
            }

            if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
                return new RegisterResponse(false, "Confirm password is required!");
            }

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return new RegisterResponse(false, "Passwords do not match!");
            }

            if (request.getRole() == null || request.getRole().isBlank()) {
                return new RegisterResponse(false, "Role is required!");
            }

            User user;

            if ("SELLER".equalsIgnoreCase(request.getRole())) {
                if (request.getStoreName() == null || request.getStoreName().isBlank()) {
                    return new RegisterResponse(false, "Store name is required for seller!");
                }

                Seller seller = new Seller();
                seller.setStoreName(request.getStoreName());
                user = seller;
                user.setRole("SELLER");

            } else if ("BIDDER".equalsIgnoreCase(request.getRole())) {
                Bidder bidder = new Bidder();
                bidder.setBalance(0.0);
                user = bidder;
                user.setRole("BIDDER");

            } else {
                return new RegisterResponse(false, "Invalid role!");
            }

            user.setFullName(request.getFullName());
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());

            register(user);

            return new RegisterResponse(true, "Register success!");

        } catch (AuctionException e) {
            return new RegisterResponse(false, e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return new RegisterResponse(false, "Register failed!");
        }
    }
}
