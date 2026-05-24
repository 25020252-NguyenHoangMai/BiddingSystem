package com.auction.server;

import com.auction.server.controller.*;
import com.auction.server.dao.*;
import com.auction.server.factory.ItemFactoryRegistry;
import com.auction.server.network.SocketServer;
import com.auction.server.realtime.DashboardObserver;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.*;

public class    MainServer {
    public static void main(String[] args) {
//        ItemFactoryRegistry.initializeDefaultFactories();

        ItemDAO itemDAO = new ItemDAO();
        UserDAO userDAO = new UserDAO();
        BidDAO bidDAO = new BidDAO();
        AutoBidDAO autoBidDAO = new AutoBidDAO();
        SessionDAO sessionDAO = new SessionDAO(itemDAO);

        SessionWatchRegistry sessionWatchRegistry = new SessionWatchRegistry();
        DashboardWatchRegistry dashboardWatchRegistry = new DashboardWatchRegistry();

        ImageStorageService imageStorageService = new ImageStorageService();
        UserService userService = new UserService(userDAO, sessionDAO);
        ItemService itemService = new ItemService(itemDAO, userService, sessionDAO, bidDAO);

        BidHistoryService bidHistoryService = new BidHistoryService(bidDAO, userService);
        BidIncrementService bidIncrementService = new BidIncrementService();
        BidReservationCalculator bidReservationCalculator = new BidReservationCalculator();
        BidValidationService bidValidationService = new BidValidationService(userService, bidIncrementService);
        BidTransactionExecutor bidTransactionExecutor = new BidTransactionExecutor(bidDAO, sessionDAO, userDAO,
                                                    bidValidationService, bidReservationCalculator);

        AntiSnipingService antiSnipingService = new AntiSnipingService();
        SessionService sessionService = new SessionService(sessionDAO, userDAO, bidDAO);
        AutoBiddingService autoBiddingService = new AutoBiddingService(bidValidationService, sessionService,
                                                    bidIncrementService, bidTransactionExecutor, userService,
                                                    autoBidDAO, antiSnipingService);
        BiddingService biddingService = new BiddingService(sessionService, antiSnipingService, userService,
                                            bidValidationService, bidTransactionExecutor);

        AuthController authController = new AuthController(userService);
        ItemController itemController = new ItemController(itemService, sessionService,dashboardWatchRegistry,
                                            sessionWatchRegistry, imageStorageService);
        BiddingController biddingController = new BiddingController(biddingService, sessionService,
                                                sessionWatchRegistry, autoBiddingService, bidIncrementService,
                                                userService, bidHistoryService);
        RealTimeController realTimeController = new RealTimeController(sessionWatchRegistry, dashboardWatchRegistry,
                                                sessionService, userService, bidIncrementService);

        AuctionController auctionController = new AuctionController(authController, itemController, biddingController,
                                                realTimeController);

        AuctionSessionScheduler sessionScheduler = new AuctionSessionScheduler(sessionService,
                                                                sessionWatchRegistry, dashboardWatchRegistry,
                                                                itemService, userService);
        sessionScheduler.start();

        SocketServer server = new SocketServer(5000, auctionController, sessionWatchRegistry,
                                dashboardWatchRegistry);
        server.startServer();
    }
}