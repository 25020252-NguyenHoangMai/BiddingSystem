package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.request.LoginRequest;
import com.auction.request.RegisterRequest;
import com.auction.response.LoginResponse;
import com.auction.response.RegisterResponse;

public class AuthService {
    private static AuthService instance;
    private final ClientSocket clientSocket = new ClientSocket();

    private AuthService() {}

    public static AuthService getInstance() {
        if (instance == null) {
            instance = new AuthService();
        }
        return instance;
    }

    /**
     * Xử lý gửi yêu cầu đăng ký
     */
    public RegisterResponse register(String fullName, String username, String password, String confirmPassword) throws Exception {
        clientSocket.connect();

        RegisterRequest request = new RegisterRequest(fullName, username, password, confirmPassword);

        clientSocket.sendRequest(request);

        Object response = clientSocket.receiveResponse();

        if (response instanceof RegisterResponse regRes) {
            return regRes;
        }

        throw new IllegalStateException(
                "Invalid server response: "
                        + (response == null
                        ? "null"
                        : response.getClass().getSimpleName())
        );
    }

    /**
     * Xử lý gửi yêu cầu đăng nhập
     */
    public LoginResponse login(String username, String password) throws Exception {
        clientSocket.connect();

        LoginRequest request = new LoginRequest(username, password);

        clientSocket.sendRequest(request);

        Object response = clientSocket.receiveResponse();

        if (response instanceof LoginResponse loginRes) {
            return loginRes;
        }

        throw new IllegalStateException(
                "Invalid server response: "
                        + (response == null
                        ? "null"
                        : response.getClass().getSimpleName())
        );
    }
}