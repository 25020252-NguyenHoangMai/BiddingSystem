package com.auction.server.controller;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.dto.UserSessionDTO;
import com.auction.request.DepositRequest;
import com.auction.request.EnableSellerRequest;
import com.auction.request.LoginRequest;
import com.auction.request.RegisterRequest;
import com.auction.response.DepositResponse;
import com.auction.response.EnableSellerResponse;
import com.auction.response.LoginResponse;
import com.auction.response.RegisterResponse;
import com.auction.server.service.UserService;
//import com.auction.server.factory.UserRegistrationFactory;

public class AuthController {
    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    public LoginResponse login(LoginRequest request) {
        try {
            User user = userService.login(request.getUsername(), request.getPassword());
            UserSessionDTO userSession = toUserSessionDTO(user);

            return new LoginResponse(
                    true,
                    "Dang nhap thanh cong!",
                    userSession
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
            if (request.getConfirmPassword() == null || request.getConfirmPassword().isBlank()) {
                return new RegisterResponse(false, "Confirm password is required!");
            }

            if (request.getPassword() == null) {
                return new RegisterResponse(false, "Password is required!");
            }

            if (!request.getPassword().equals(request.getConfirmPassword())) {
                return new RegisterResponse(false, "Passwords do not match!");
            }

            Bidder bidder = new Bidder();
            bidder.setRole("BIDDER");
            bidder.setBalance(0.0);
            bidder.setSellerEnabled(false);

            User user = bidder;
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

    // ===== ENABLE SELLER =====
    public EnableSellerResponse enableSeller(EnableSellerRequest request) {
        try {
            if (request.getUserId() == null || request.getUserId().isBlank())
                return new EnableSellerResponse(false, "User ID is required!", null);

            User updated = userService.enableSeller(request.getUserId());
            if (updated == null)
                return new EnableSellerResponse(false, "User not found!", null);

            UserSessionDTO dto = toUserSessionDTO(updated);

            return new EnableSellerResponse(true, "Seller enabled successfully!", dto);

        } catch (AuctionException e) {
            return new EnableSellerResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            e.printStackTrace();
            return new EnableSellerResponse(false, "Failed to enable seller!", null);
        }
    }

    public DepositResponse deposit(DepositRequest request) {
        try {
            if (request.getUserId() == null || request.getUserId().isBlank()) {
                return new DepositResponse(false, "User ID is required!", null);
            }

            if (request.getAmount() <= 0) {
                return new DepositResponse(false, "Deposit amount must be greater than zero!", null);
            }

            User updated = userService.deposit(request.getUserId(), request.getAmount());
            UserSessionDTO dto = toUserSessionDTO(updated);

            return new DepositResponse(true, "Deposit successfully!", dto);

        } catch (AuctionException e) {
            return new DepositResponse(false, e.getMessage(), null);
        } catch (Exception e) {
            e.printStackTrace();
            return new DepositResponse(false, "Deposit failed!", null);
        }
    }

    private UserSessionDTO toUserSessionDTO(User user) {
        UserSessionDTO dto = new UserSessionDTO();

        dto.setId(user.getId());
        dto.setUsername(user.getUsername());
        dto.setFullName(user.getFullName());
        dto.setRole(user.getRole());

        if (user instanceof Bidder bidder) {
            dto.setBalance(bidder.getBalance());
            dto.setSellerEnabled(bidder.isSellerEnabled());
        }

        return dto;
    }
}
