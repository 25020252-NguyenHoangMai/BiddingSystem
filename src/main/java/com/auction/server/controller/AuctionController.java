package com.auction.server.controller;

import com.auction.request.*;
import com.auction.response.ErrorResponse;
import com.auction.response.GetBidHistoryResponse;
import com.auction.response.Response;
import com.auction.server.realtime.AuctionSessionObserver;
import com.auction.server.realtime.DashboardObserver;

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
        if (request instanceof DepositRequest depositRequest) {
            return authController.deposit(depositRequest);
        }
        if (request instanceof EnableSellerRequest enableSellerRequest) {
            return authController.enableSeller(enableSellerRequest);
        }
        if (request instanceof GetCurrentUserRequest getCurrentUserRequest) {
            return authController.getCurrentUser(getCurrentUserRequest);
        }
        if (request instanceof GetAllUsersRequest getAllUsersRequest) {
            return authController.getAllUsers(getAllUsersRequest);
        }
        if (request instanceof DeleteUserRequest deleteUserRequest) {
            return authController.deleteUser(deleteUserRequest);
        }
        if (request instanceof EditProfileRequest editProfileRequest) {
            return authController.editProfile(editProfileRequest);
        }

        if (request instanceof GetAllItemsRequest getAllItemsRequest) {
            return itemController.getAllItems(getAllItemsRequest);
        }
        if (request instanceof AddItemRequest addItemRequest) {
            return itemController.addItem(addItemRequest);
        }
        if (request instanceof GetAuctionDetailRequest getAuctionDetailRequest) {
            return itemController.getAuctionDetail(getAuctionDetailRequest);
        }
        if (request instanceof AdminCancelAuctionRequest adminCancelAuctionRequest) {
            return itemController.adminCancelAuction(adminCancelAuctionRequest);
        }
        if (request instanceof SellerCancelAuctionRequest sellerCancelAuctionRequest) {
            return itemController.sellerCancelAuction(sellerCancelAuctionRequest);
        }
        if (request instanceof SellerUpdateItemRequest sellerUpdateItemRequest) {
            return itemController.sellerUpdateItem(sellerUpdateItemRequest);
        }
        if (request instanceof SellerUpdateAuctionTimeRequest sellerUpdateAuctionTimeRequest) {
            return itemController.sellerUpdateAuctionTime(sellerUpdateAuctionTimeRequest);
        }
        if (request instanceof GetItemImageRequest getItemImageRequest) {
            return itemController.getItemImage(getItemImageRequest);
        }
        if (request instanceof GetSellerHistoryRequest getSellerHistoryRequest) {
            return itemController.getSellerHistory(getSellerHistoryRequest);
        }

        if (request instanceof PlaceBidRequest placeBidRequest) {
            return biddingController.placeBid(placeBidRequest);
        }
        if (request instanceof SetAutoBidRequest setAutoBidRequest) {
            return biddingController.setAutoBid(setAutoBidRequest);
        }
        if (request instanceof GetBidHistoryRequest getBidHistoryRequest) {
            return biddingController.getBidHistory(getBidHistoryRequest);
        }
        if (request instanceof GetSessionHistoryRequest getSessionHistoryRequest) {
            return biddingController.getSessionHistory(getSessionHistoryRequest);
        }

        if (request instanceof WatchSessionRequest watchSessionRequest) {
            return realTimeController.watchSession(watchSessionRequest, observer);
        }
        if (request instanceof UnwatchSessionRequest unwatchSessionRequest) {
            return realTimeController.unwatchSession(unwatchSessionRequest, observer);
        }
        if (request instanceof WatchDashboardRequest watchDashboardRequest) {
            if (observer instanceof DashboardObserver dashboardObserver) {
                return realTimeController.watchDashboard(watchDashboardRequest, dashboardObserver);
            }
            return new ErrorResponse("Dashboard observer is required");
        }
        if (request instanceof UnwatchDashboardRequest unwatchDashboardRequest) {
            if (observer instanceof DashboardObserver dashboardObserver) {
                return realTimeController.unwatchDashboard(unwatchDashboardRequest, dashboardObserver);
            }
            return new ErrorResponse("Dashboard observer is required");
        }

        return new ErrorResponse("Unknown request");
    }
}