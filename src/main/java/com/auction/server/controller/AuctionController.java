package com.auction.server.controller;

import com.auction.request.LoginRequest;
import com.auction.request.RegisterRequest;
import com.auction.request.Request;
import com.auction.response.Response;
import com.auction.server.service.UserService;

public class AuctionController {

    private UserService userService;

    public AuctionController() {
        this.userService = new UserService();
    }

    public Response handleRequest(Request request) {
        if (request instanceof LoginRequest loginRequest) {
            return userService.login(loginRequest);
        }

        if (request instanceof RegisterRequest registerRequest) {
            return userService.register(registerRequest);
        }
        return new Response(false, "Unknown request") {};
    }
}