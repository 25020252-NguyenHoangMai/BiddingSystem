package com.auction.server.network;

import com.auction.server.controller.AuctionController;
import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;

public class SocketServer {
    private final int port;
    private final AuctionController auctionController;

    public SocketServer(int port, AuctionController auctionController) {
        this.port = port;
        this.auctionController = auctionController;
    }

    public void startServer() {
        try {
            ServerSocket serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (true) {
                Socket socket = serverSocket.accept();
                System.out.println("Client connected: " + socket);

                ClientHandler handler = new ClientHandler(socket, auctionController);
                handler.start();
            }

        } catch (IOException e) {
            System.out.println("Failed to start server on port " + port);
            e.printStackTrace();
        }
    }
}
