package com.auction.server.network;

import com.auction.server.controller.AuctionController;
import com.auction.server.realtime.SessionWatchRegistry;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class SocketServer {
    private final int port;
    private final AuctionController auctionController;
    private final SessionWatchRegistry sessionWatchRegistry;

    public SocketServer(int port, AuctionController auctionController, SessionWatchRegistry sessionWatchRegistry) {
        this.port = port;
        this.auctionController = auctionController;
        this.sessionWatchRegistry = sessionWatchRegistry;
    }

    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket);

                ClientHandler handler = new ClientHandler(socket, auctionController, sessionWatchRegistry);
                handler.start();
            }

        } catch (IOException e) {
            System.out.println("Failed to start server on port " + port);
            e.printStackTrace();
        }
    }
}
