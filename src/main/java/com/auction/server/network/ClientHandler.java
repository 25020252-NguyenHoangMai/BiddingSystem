package com.auction.server.network;

import com.auction.request.Request;
import com.auction.response.Response;
import com.auction.server.controller.AuctionController;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;

public class ClientHandler extends Thread {
    private Socket socket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    private AuctionController auctionController;

    public ClientHandler(Socket socket) {
        this.socket = socket;
        this.auctionController = new AuctionController();

        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                Object obj = in.readObject();

                if (obj instanceof Request request) {
                    System.out.println("Received request: " + request.getClass().getSimpleName());

                    Response response = auctionController.handleRequest(request);

                    out.writeObject(response);
                    out.flush();
                } else {
                    System.out.println("Unknown object type: " + obj);
                }
            }
        } catch (Exception e) {
            System.out.println("Client disconnected: " + socket);
        }
    }
}