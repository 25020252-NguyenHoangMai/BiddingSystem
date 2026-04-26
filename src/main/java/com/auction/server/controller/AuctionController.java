package com.auction.server.controller;

import com.auction.request.GetAllItemsRequest;
import com.auction.request.LoginRequest;
import com.auction.request.RegisterRequest;
import com.auction.request.Request;
import com.auction.response.ErrorResponse;
import com.auction.response.Response;

public class AuctionController {

    private final AuthController authController;
    private final ItemController itemController;

    public AuctionController(AuthController authController, ItemController itemController) {
        this.authController = authController;
        this.itemController = itemController;
    }

    public Response handleRequest(Request request) {
        if (request instanceof LoginRequest loginRequest) { // Pattern matching for instanceof: kiểm tra + gán biến + ép kiểu
            return authController.login(loginRequest);
        }

        if (request instanceof RegisterRequest registerRequest) {
            return authController.register(registerRequest);
        }

        if (request instanceof GetAllItemsRequest getAllItemsRequest) {
            return itemController.getAllItems(getAllItemsRequest);
        }
        return new ErrorResponse("Unknown request");
    }
}