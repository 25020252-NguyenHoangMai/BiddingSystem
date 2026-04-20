package com.auction.server;

import com.auction.server.controller.AuctionController;
import com.auction.server.network.SocketServer;
import com.auction.server.service.AntiSnipingService;
import com.auction.server.service.AutoBiddingService;
import com.auction.server.service.BiddingService;
import com.auction.server.service.SessionService;
import com.auction.server.service.UserService;

import com.auction.server.network.SocketServer;

public class MainServer {
    public static void main(String[] args) {
        UserService userService = new UserService();
        SessionService sessionService = new SessionService();
        AutoBiddingService autoBiddingService = new AutoBiddingService();
        AntiSnipingService antiSnipingService = new AntiSnipingService();
        //BiddingService biddingService = new BiddingService(sessionService, autoBiddingService, antiSnipingService);

        //AuctionController auctionController = new AuctionController(userService, sessionService, biddingService, autoBiddingService, antiSnipingService);

        SocketServer server = new SocketServer();
        server.startServer();
    }
}