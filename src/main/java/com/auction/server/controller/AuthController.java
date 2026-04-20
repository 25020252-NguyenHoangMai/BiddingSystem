package com.auction.server.controller;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.request.LoginRequest;
import com.auction.request.RegisterRequest;
import com.auction.response.LoginResponse;
import com.auction.response.RegisterResponse;
import com.auction.server.service.UserService;

public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            if (request.getUsername() == null || request.getUsername().isBlank()) {
                return new LoginResponse(false, "Username is required!", null);
            }

            if (request.getPassword() == null || request.getPassword().isBlank()) {
                return new LoginResponse(false, "Password is required!", null);
            }

            User user = userService.login(request.getUsername(), request.getPassword());

            return new LoginResponse(
                    true,
                    "Dang nhap thanh cong!",
                    user
            );

        } catch (AuthenticationException e) {
            return new LoginResponse(false, e.getMessage(), null);

        } catch (Exception e) {
            e.printStackTrace();
            return new LoginResponse(false, "Co loi xay ra khi dang nhap!", null);
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
                seller.setRole("SELLER");
                user = seller;

            } else if ("BIDDER".equalsIgnoreCase(request.getRole())) {
                Bidder bidder = new Bidder();
                bidder.setBalance(0.0);
                bidder.setRole("BIDDER");
                user = bidder;

            } else {
                return new RegisterResponse(false, "Invalid role!");
            }

            user.setFullName(request.getFullName());
            user.setUsername(request.getUsername());
            user.setPassword(request.getPassword());

            userService.register(user);

            return new RegisterResponse(true, "Register success!");

        } catch (AuctionException e) {
            return new RegisterResponse(false, e.getMessage());

        } catch (Exception e) {
            e.printStackTrace();
            return new RegisterResponse(false, "Register failed!");
        }
    }
}
