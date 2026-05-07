package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.controller.AuthController;
import com.auction.server.controller.BiddingController;
import com.auction.server.controller.ItemController;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.network.SocketServer;
import com.auction.server.service.*;

public class    MainServer {
    public static void main(String[] args) {
        ItemDAO itemDAO = new ItemDAO();
        UserDAO userDAO = new UserDAO();
        BidDAO bidDAO = new BidDAO();
        SessionDAO sessionDAO = new SessionDAO(itemDAO);

        UserService userService = new UserService(userDAO);
        ItemService itemService = new ItemService(itemDAO, userService, sessionDAO);

        BidIncrementService bidIncrementService = new BidIncrementService();
        AutoBiddingService autoBiddingService = new AutoBiddingService();
        AntiSnipingService antiSnipingService = new AntiSnipingService();
        SessionService sessionService = new SessionService(sessionDAO);
        BiddingService biddingService = new BiddingService(sessionService, bidDAO, antiSnipingService,
                                            userService, bidIncrementService, sessionDAO, userDAO);

        AuthController authController = new AuthController(userService);
        ItemController itemController = new ItemController(itemService);
        BiddingController biddingController = new BiddingController(biddingService);

        AuctionController auctionController = new AuctionController(authController, itemController, biddingController);

        SocketServer server = new SocketServer(5000, auctionController);
        server.startServer();
    }
}