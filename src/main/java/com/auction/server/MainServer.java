package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BiddingController;
import com.auction.server.controller.ItemController;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.SessionDAO;
import com.auction.server.network.SocketServer;
import com.auction.server.service.*;

public class MainServer {
    public static void main(String[] args) {
        UserService userService = new UserService();
        ItemService itemService = new ItemService();
        ItemDAO itemDAO = new ItemDAO();
        SessionDAO sessionDAO = new SessionDAO(itemDAO);
        SessionService sessionService = new SessionService(sessionDAO);
        BiddingService biddingService = new BiddingService(sessionService);

        AuthController authController = new AuthController(userService);
        ItemController itemController = new ItemController(itemService);
        BiddingController biddingController = new BiddingController(biddingService);

        //SessionService sessionService = new SessionService();
        AutoBiddingService autoBiddingService = new AutoBiddingService();
        AntiSnipingService antiSnipingService = new AntiSnipingService();
        //BiddingService biddingService = new BiddingService(sessionService, autoBiddingService, antiSnipingService);

        AuctionController auctionController = new AuctionController(authController, itemController, biddingController);

        SocketServer server = new SocketServer(5000, auctionController);
        server.startServer();
    }
}