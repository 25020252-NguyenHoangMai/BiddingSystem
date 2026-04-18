package com.auction.server.network;

import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer {

    public void startServer() {
        try {
            ServerSocket server = new ServerSocket(5000);
            System.out.println("Server started on port 5000");

            while (true) {
                Socket socket = server.accept();
                System.out.println("Client connected: " + socket);

                ClientHandler handler = new ClientHandler(socket);
                handler.start();
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
