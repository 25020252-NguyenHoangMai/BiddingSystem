package com.auction.server.controller;

import com.auction.request.*;
import com.auction.response.ErrorResponse;
import com.auction.response.Response;
import com.auction.server.realtime.AuctionSessionObserver;

public class AuctionController {

    private final AuthController authController;
    private final ItemController itemController;
    private final BiddingController biddingController;
    private final RealTimeController realTimeController;

    public AuctionController(AuthController authController, ItemController itemController,
                             BiddingController biddingController, RealTimeController realTimeController) {
        this.authController = authController;
        this.itemController = itemController;
        this.biddingController = biddingController;
        this.realTimeController = realTimeController;
    }

    public Response handleRequest(Request request, AuctionSessionObserver observer) {
        if (request instanceof LoginRequest loginRequest) { // Pattern matching for instanceof: kiểm tra + gán biến + ép kiểu
            return authController.login(loginRequest);
        }
        if (request instanceof RegisterRequest registerRequest) {
            return authController.register(registerRequest);
        }

        if (request instanceof EnableSellerRequest enableSellerRequest) {
            return authController.enableSeller(enableSellerRequest);
        }

        if (request instanceof GetAllItemsRequest getAllItemsRequest) {
            return itemController.getAllItems(getAllItemsRequest);
        }
        if (request instanceof AddItemRequest addItemRequest) {
            return itemController.addItem(addItemRequest);
        }

        if (request instanceof PlaceBidRequest placeBidRequest) {
            return biddingController.placeBid(placeBidRequest);
        }

        if (request instanceof WatchSessionRequest watchSessionRequest) {
            return realTimeController.watchSession(watchSessionRequest, observer);
        }
        if (request instanceof UnwatchSessionRequest unwatchSessionRequest) {
            return realTimeController.unwatchSession(unwatchSessionRequest, observer);
        }
        return new ErrorResponse("Unknown request");
    }
}