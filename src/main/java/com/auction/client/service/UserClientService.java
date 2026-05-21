package com.auction.client.service;

import com.auction.client.ClientSession;
import com.auction.client.network.ClientSocket;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.request.*;
import com.auction.response.*;

import java.util.ArrayList;
import java.util.List;

public class UserClientService {
    private UserClientService() {}

    private static final UserClientService INSTANCE = new UserClientService();
    public static UserClientService getInstance() { return INSTANCE; }

    // Lấy toàn bộ danh sách người dùng từ Server
    public List<UserSessionDTO> getAllUsers() {
        ClientSocket socket = ClientSocket.getInstance();

        try {
            String requesterId = ClientSession.getCurrentUser().getId();

            GetAllUsersResponse response =
                    socket.sendRequestAndWait(new GetAllUsersRequest(requesterId), GetAllUsersResponse.class);

            if (!response.isSuccess()) {
                throw new RuntimeException(response.getMessage());
            }

            return response.getUsers() != null
                    ? response.getUsers()
                    : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("[UserClientService] getAllUsers failed: " + e.getMessage());

            throw new RuntimeException("Unable to load user list", e);
        }
    }

    // Gửi yêu cầu xóa người dùng theo ID
    public boolean deleteUser(String requesterId, String targetUserId) {
        ClientSocket socket = ClientSocket.getInstance();

        try {
            DeleteUserResponse response =
                    socket.sendRequestAndWait(
                            new DeleteUserRequest(requesterId, targetUserId), DeleteUserResponse.class);

            return response.isSuccess();

        } catch (Exception e) {
            System.err.println("[UserClientService] deleteUser failed: " + e.getMessage());
            e.printStackTrace();

            return false;
        }
    }

    // Gửi EnableSellerRequest lên Server
    public UserSessionDTO enableSeller(String userId) throws Exception {
        ClientSocket socket = ClientSocket.getInstance();

        EnableSellerResponse response =
                socket.sendRequestAndWait(
                        new EnableSellerRequest(userId),
                        EnableSellerResponse.class
                );

        if (!response.isSuccess()) {
            throw new Exception(response.getMessage());
        }

        return response.getUserSession();
    }

    // Gửi DepositRequest
    public UserSessionDTO deposit(String userId, double amount) throws Exception {
        ClientSocket socket = ClientSocket.getInstance();

        DepositResponse response =
                socket.sendRequestAndWait(
                        new DepositRequest(userId, amount),
                        DepositResponse.class
                );

        if (!response.isSuccess()) {
            throw new Exception(response.getMessage());
        }

        UserSessionDTO user = response.getUserSession();

        if (user == null) {
            throw new IllegalStateException("DepositResponse userSession is null");
        }

        return user;
    }

    public UserSessionDTO getCurrentUser(String userId) throws Exception {
        ClientSocket socket = ClientSocket.getInstance();

        GetCurrentUserResponse response =
                socket.sendRequestAndWait(
                        new GetCurrentUserRequest(userId),
                        GetCurrentUserResponse.class
                );

        if (!response.isSuccess()) {
            throw new Exception(response.getMessage());
        }

        if (response.getUserSession() == null) {
            throw new IllegalStateException("GetCurrentUserResponse userSession is null");
        }

        return response.getUserSession();
    }
}
