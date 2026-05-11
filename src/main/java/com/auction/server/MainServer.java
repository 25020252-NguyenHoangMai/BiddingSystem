package com.auction.server;

import com.auction.server.controller.*;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.network.SocketServer;
import com.auction.server.realtime.DashboardObserver;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.*;

public class    MainServer {
    public static void main(String[] args) {
        ItemDAO itemDAO = new ItemDAO();
        UserDAO userDAO = new UserDAO();
        BidDAO bidDAO = new BidDAO();
        SessionDAO sessionDAO = new SessionDAO(itemDAO);

        SessionWatchRegistry sessionWatchRegistry = new SessionWatchRegistry();
        DashboardWatchRegistry dashboardWatchRegistry = new DashboardWatchRegistry();

        UserService userService = new UserService(userDAO);
        ItemService itemService = new ItemService(itemDAO, userService, sessionDAO);

        BidIncrementService bidIncrementService = new BidIncrementService();
        AutoBiddingService autoBiddingService = new AutoBiddingService();
        AntiSnipingService antiSnipingService = new AntiSnipingService();
        SessionService sessionService = new SessionService(sessionDAO);
        BiddingService biddingService = new BiddingService(sessionService, bidDAO, antiSnipingService,
                                            userService, bidIncrementService, sessionDAO, userDAO);

        AuthController authController = new AuthController(userService);
        ItemController itemController = new ItemController(itemService, sessionService, userService,dashboardWatchRegistry);
        BiddingController biddingController = new BiddingController(biddingService, sessionService, sessionWatchRegistry);
        RealTimeController realTimeController = new RealTimeController(sessionWatchRegistry, dashboardWatchRegistry,
                                                sessionService, userService);

        AuctionController auctionController = new AuctionController(authController, itemController, biddingController,
                                                realTimeController);

        SocketServer server = new SocketServer(5000, auctionController, sessionWatchRegistry,
                                dashboardWatchRegistry);
        server.startServer();
    }
}