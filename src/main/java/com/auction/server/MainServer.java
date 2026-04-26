package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.ItemController;
import com.auction.server.network.SocketServer;
import com.auction.server.service.AntiSnipingService;
import com.auction.server.service.AutoBiddingService;
import com.auction.server.service.ItemService;
import com.auction.server.service.UserService;

public class MainServer {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ItemService itemService = new ItemService();

        AuthController authController = new AuthController(userService);
        ItemController itemController = new ItemController(itemService);

        //SessionService sessionService = new SessionService();
        AutoBiddingService autoBiddingService = new AutoBiddingService();
        AntiSnipingService antiSnipingService = new AntiSnipingService();
        //BiddingService biddingService = new BiddingService(sessionService, autoBiddingService, antiSnipingService);

        AuctionController auctionController = new AuctionController(authController,  itemController);

        SocketServer server = new SocketServer(5000, auctionController);
        server.startServer();
    }
}