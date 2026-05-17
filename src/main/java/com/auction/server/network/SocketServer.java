package com.auction.server.network;

import com.auction.server.controller.AuctionController;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.realtime.SessionWatchRegistry;

import java.net.ServerSocket;
import java.net.Socket;
import java.io.IOException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

public class SocketServer {
    private static final int MAX_CLIENTS = 10;
    private final int port;
    private final AuctionController auctionController;
    private final SessionWatchRegistry sessionWatchRegistry;
    private final DashboardWatchRegistry dashboardWatchRegistry;
    private final ExecutorService clientExecutor = Executors.newVirtualThreadPerTaskExecutor();
    private final AtomicInteger activeClients = new AtomicInteger(0);

    private ServerSocket serverSocket;
    private volatile boolean running;

    public SocketServer(int port, AuctionController auctionController, SessionWatchRegistry sessionWatchRegistry,
                        DashboardWatchRegistry dashboardWatchRegistry) {
        this.port = port;
        this.auctionController = auctionController;
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.dashboardWatchRegistry = dashboardWatchRegistry;
    }

    public void startServer() {
        running = true;
        try {
            serverSocket = new ServerSocket(port);
            System.out.println("Server started on port " + port);

            while (running) {
                try {
                    Socket socket = serverSocket.accept();
                    if (activeClients.incrementAndGet() > MAX_CLIENTS) {
                        activeClients.decrementAndGet();
                        System.out.println("Rejecting client because server is full: " + socket);
                        socket.close();
                        continue;
                    }
                    System.out.println("Client connected: " + socket + ". Active clients: " + activeClients.get());

                    ClientHandler handler = new ClientHandler(socket, auctionController, sessionWatchRegistry,
                                                dashboardWatchRegistry);

                    try {
                        clientExecutor.submit(() -> {
                            try {
                                handler.run();
                            } finally {
                                int remainingClients = activeClients.decrementAndGet();
                                System.out.println("Client disconnected: " + socket + ". Active clients: " + remainingClients);
                            }
                        });
                    } catch (RuntimeException e) {
                        activeClients.decrementAndGet();
                        System.out.println("Failed to submit client handler: " + socket);

                        try {
                            socket.close();
                        } catch (IOException closeException) {
                            System.out.println("Failed to close rejected client socket: " + socket);
                        }
                    }
                } catch (IOException e) {
                    if (running) {
                        System.out.println("Error while accepting client connection");
                        e.printStackTrace();
                    }
                }
            }

        } catch (IOException e) {
            System.out.println("Failed to start server on port " + port);
            e.printStackTrace();
        } finally {
            stopServer();
        }
    }

    public void stopServer() {
        if (!running) {
            return;
        }
        running = false;
        try {
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
        } catch (IOException e) {
            System.out.println("Error while closing server socket");
            e.printStackTrace();
        }

        clientExecutor.shutdown();

        System.out.println("Server stopped");
    }
}
