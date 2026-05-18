package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.UserSessionDTO;
import com.auction.request.DepositRequest;
import com.auction.request.EnableSellerRequest;
import com.auction.request.GetCurrentUserRequest;
import com.auction.response.DepositResponse;
import com.auction.response.EnableSellerResponse;
import com.auction.response.GetCurrentUserResponse;

import java.util.List;

public class UserClientService {
    private UserClientService() {}

    private static final UserClientService INSTANCE = new UserClientService();
    public static UserClientService getInstance() { return INSTANCE; }

    // Lấy toàn bộ danh sách người dùng từ Server
    public List<UserSessionDTO> getAllUsers() {
        ClientSocket socket = ClientSocket.getInstance();
        socket.connect();
        try {
            // gửi request
            socket.sendRequest("GET_ALL_USERS");

            // nhận response
            Object res = socket.receiveResponse();

            // ép kiểu an toàn
            if (res instanceof List<?> rawList) {
                return rawList.stream()
                        .filter(UserSessionDTO.class::isInstance)
                        .map(UserSessionDTO.class::cast)
                        .toList();
            }

            System.err.println(
                    "[AdminService] Invalid response type: "
                            + (res == null
                            ? "null"
                            : res.getClass().getSimpleName())
            );
        } catch (Exception e) {
            System.err.println("[AdminService] getAllUsers failed: " + e.getMessage());
            e.printStackTrace();
        }
        return List.of();
    }

    // Gửi yêu cầu xóa người dùng theo ID
    public boolean deleteUser(int userId) {
        ClientSocket socket = ClientSocket.getInstance();
        socket.connect();
        try {
            socket.sendRequest("DELETE_USER:" + userId);

            Object res = socket.receiveResponse();

            if (res instanceof Boolean success) {
                return success;
            }

            System.err.println(
                    "[AdminService] Invalid delete response type: "
                            + (res == null
                            ? "null"
                            : res.getClass().getSimpleName())
            );
        } catch (Exception e) {
            System.err.println("[AdminService] deleteUser failed: " + e.getMessage());
            e.printStackTrace();
        }

        return false;
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
