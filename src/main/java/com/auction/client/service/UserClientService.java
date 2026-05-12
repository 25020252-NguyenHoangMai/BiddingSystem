package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.UserSessionDTO;
import com.auction.request.DepositRequest;
import com.auction.request.EnableSellerRequest;
import com.auction.response.DepositResponse;
import com.auction.response.EnableSellerResponse;

import java.util.List;

public class UserClientService {

    // Áp dụng Singleton để AdminController dễ gọi
    private static UserClientService instance;

    private UserClientService() {}

    public static UserClientService getInstance() {
        if (instance == null) {
            instance = new UserClientService();
        }
        return instance;
    }

    // Lấy toàn bộ danh sách người dùng từ Server
    public List<UserSessionDTO> getAllUsers() {
        ClientSocket socket = ClientSocket.getInstance();
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
    public boolean enableSeller(String userId) throws Exception {
        ClientSocket socket = ClientSocket.getInstance();
        socket.connect();

        socket.sendRequest(new EnableSellerRequest(userId));
        Object raw = socket.receiveResponse();

        if (!(raw instanceof EnableSellerResponse res)) {
            throw new IllegalStateException("Expected EnableSellerResponse but got: "
                            + (raw == null
                            ? "null"
                            : raw.getClass().getSimpleName())
            );
        }

        if (!res.isSuccess()) {
            throw new Exception(res.getMessage());
        }

        return true;
    }

    // Gửi DepositRequest
    public UserSessionDTO deposit(String userId, double amount) throws Exception {
        ClientSocket socket = ClientSocket.getInstance();
        socket.connect();

        socket.sendRequest(new DepositRequest(userId, amount));
        Object raw = socket.receiveResponse();

        if (!(raw instanceof DepositResponse res)) {
            throw new IllegalStateException("Expected DepositResponse but got: "
                            + (raw == null
                            ? "null"
                            : raw.getClass().getSimpleName())
            );
        }

        if (!res.isSuccess()) {
            throw new Exception(res.getMessage());
        }

        UserSessionDTO user = res.getUserSession();

        if (user == null) {
            throw new IllegalStateException("DepositResponse userSession is null");
        }

        return user;
    }
}
