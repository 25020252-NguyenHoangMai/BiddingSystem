package com.auction.server.controller;

import com.auction.request.*;
import com.auction.response.ErrorResponse;
import com.auction.response.Response;

public class AuctionController {

    private final AuthController authController;
    private final ItemController itemController;
    private final BiddingController biddingController;

    public AuctionController(AuthController authController, ItemController itemController,
                             BiddingController biddingController) {
        this.authController = authController;
        this.itemController = itemController;
        this.biddingController = biddingController;
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

        if (request instanceof PlaceBidRequest placeBidRequest) {
            return biddingController.placeBid(placeBidRequest);
        }
        return new ErrorResponse("Unknown request");
    }
}