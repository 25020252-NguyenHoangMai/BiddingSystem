package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.request.LoginRequest;
import com.auction.response.LoginResponse;

public class AuthService {
    public LoginResponse login(String username, String password) throws Exception {
        ClientSocket clientSocket = ClientSocket.getInstance();
        clientSocket.connect();

        LoginRequest request = new LoginRequest(username, password);
        clientSocket.sendRequest(request);

        Object response = clientSocket.receiveResponse();

        if (response instanceof LoginResponse) {
            return (LoginResponse) response;
        }
        return null;
    }
}
