package com.auction.server;

import com.auction.server.network.SocketServer;

public class MainServer {
    public static void main(String[] args) {
        SocketServer server = new SocketServer();
        server.startServer();
    }
}