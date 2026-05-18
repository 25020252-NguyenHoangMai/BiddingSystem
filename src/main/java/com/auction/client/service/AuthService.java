package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.request.LoginRequest;
import com.auction.request.RegisterRequest;
import com.auction.response.LoginResponse;
import com.auction.response.RegisterResponse;

public class AuthService {
    private final ClientSocket clientSocket = ClientSocket.getInstance();

    private AuthService() {}

    private static final AuthService INSTANCE = new AuthService();
    public static AuthService getInstance() { return INSTANCE; }

    /**
     * Xử lý gửi yêu cầu đăng ký
     */
    public RegisterResponse register(String fullName, String username, String password, String confirmPassword) throws Exception {

        RegisterRequest request = new RegisterRequest(fullName, username, password, confirmPassword);

        return clientSocket.sendRequestAndWait(request, RegisterResponse.class);
    }

    /**
     * Xử lý gửi yêu cầu đăng nhập
     */
    public LoginResponse login(String username, String password) throws Exception {

        LoginRequest request = new LoginRequest(username, password);

        return clientSocket.sendRequestAndWait(request, LoginResponse.class);
    }
}