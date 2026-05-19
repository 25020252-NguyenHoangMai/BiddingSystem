package com.auction.server.controller;

import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.dto.UserSessionDTO;
import com.auction.request.*;
import com.auction.response.*;
import com.auction.server.service.UserService;

import java.util.List;
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
            dto.setReservedBalance(bidder.getReservedBalance());
            dto.setSellerEnabled(bidder.isSellerEnabled());
        }

        return dto;
    }

    public GetCurrentUserResponse getCurrentUser(GetCurrentUserRequest request) {
        try {
            if (request.getUserId() == null || request.getUserId().isBlank()) {
                return new GetCurrentUserResponse(false, "User ID is required!", null);
            }

            User user = userService.getUserById(request.getUserId());
            UserSessionDTO dto = toUserSessionDTO(user);

            return new GetCurrentUserResponse(true, "Get current user successfully!", dto);
        } catch (Exception e) {
            e.printStackTrace();
            return new GetCurrentUserResponse(false, "Get current user failed!", null);
        }
    }

    public GetAllUsersResponse getAllUsers(GetAllUsersRequest request) {
        try {
            if (request.getRequesterId() == null || request.getRequesterId().isBlank()) {
                return new GetAllUsersResponse(false, "Requester ID is required!", List.of());
            }

            User requester = userService.getUserById(request.getRequesterId());
            if (requester == null) {
                return new GetAllUsersResponse(false, "Requester not found!", List.of());
            }

            if (!"ADMIN".equalsIgnoreCase(requester.getRole())) {
                return new GetAllUsersResponse(false, "Only admin can get all users!", List.of());
            }

            List<UserSessionDTO> users = userService.getAllUsers()
                    .stream()
                    .map(this::toUserSessionDTO)
                    .toList();

            return new GetAllUsersResponse(true, "Get all users successfully!", users);
        } catch (Exception e) {
            e.printStackTrace();
            return new GetAllUsersResponse(false, "Get all users failed!", List.of());
        }
    }

    public DeleteUserResponse deleteUser(DeleteUserRequest request) {
        try {
            if (request.getRequesterId() == null || request.getRequesterId().isBlank()) {
                return new DeleteUserResponse(false, "Requester ID is required!");
            }

            if (request.getTargetUserId() == null || request.getTargetUserId().isBlank()) {
                return new DeleteUserResponse(false, "Target user ID is required!");
            }

            User requester = userService.getUserById(request.getRequesterId());
            if (requester == null) {
                return new DeleteUserResponse(false, "Requester not found!");
            }

            if (!"ADMIN".equalsIgnoreCase(requester.getRole())) {
                return new DeleteUserResponse(false, "Only admin can delete users!");
            }

            if (request.getRequesterId().equals(request.getTargetUserId())) {
                return new DeleteUserResponse(false, "Admin cannot delete yourself!");
            }

            userService.deleteUser(request.getTargetUserId());
            return new DeleteUserResponse(true, "Delete user successfully!");
        } catch (Exception e) {
            e.printStackTrace();
            return new DeleteUserResponse(false, "Delete user failed!");
        }
    }

    public EditProfileResponse editProfile(EditProfileRequest request) {
        try {
            User updated = userService.updateProfile(
                    request.getUserId(),
                    request.getFullName(),
                    request.getUsername(),
                    request.getPassword()
            );

            UserSessionDTO dto = toUserSessionDTO(updated);

            return new EditProfileResponse(true, "Profile updated successfully!", dto);

        } catch (AuctionException e) {
            return new EditProfileResponse(false, e.getMessage(), null);

        } catch (Exception e) {
            e.printStackTrace();
            return new EditProfileResponse(false, "Update profile failed!", null);
        }
    }
}
