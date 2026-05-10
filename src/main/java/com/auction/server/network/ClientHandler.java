package com.auction.server.network;

import com.auction.request.Request;
import com.auction.response.BidUpdateResponse;
import com.auction.response.Response;
import com.auction.response.ErrorResponse;
import com.auction.server.controller.AuctionController;
import com.auction.server.realtime.AuctionSessionObserver;
import com.auction.server.realtime.SessionWatchRegistry;

import java.io.EOFException;
import java.io.IOException;
import java.io.InvalidClassException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.OptionalDataException;
import java.io.StreamCorruptedException;
import java.net.Socket;
import java.net.SocketException;

public class ClientHandler implements Runnable, AuctionSessionObserver {
    private final Socket socket;
    private final AuctionController auctionController;
    private final SessionWatchRegistry sessionWatchRegistry;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, AuctionController auctionController, SessionWatchRegistry sessionWatchRegistry) {
        this.socket = socket;
        this.auctionController = auctionController;
        this.sessionWatchRegistry = sessionWatchRegistry;
    }

    @Override
    public void run() {
        try {
            out = new ObjectOutputStream(socket.getOutputStream());
            out.flush();

            in = new ObjectInputStream(socket.getInputStream());
            while (true) {
                Object obj;

                try {
                    obj = in.readObject();
                } catch (EOFException | SocketException e) {
                    System.out.println("Client disconnected: " + socket);
                    break;
                } catch (StreamCorruptedException e) {
                    System.out.println("Corrupted stream from client: " + socket);
                    safeSendResponse(new ErrorResponse("Corrupted request stream"));
                    break;
                } catch (InvalidClassException e) {
                    System.out.println("Invalid class sent by client: " + socket);
                    safeSendResponse(new ErrorResponse("Invalid request class"));
                    break;
                } catch (OptionalDataException e) {
                    System.out.println("Unexpected raw data from client: " + socket);
                    safeSendResponse(new ErrorResponse("Unexpected data format"));
                    break;
                } catch (ClassNotFoundException e) {
                    System.out.println("Unknown class from client: " + socket);
                    safeSendResponse(new ErrorResponse("Unknown request type"));
                    break;
                }


                if (!(obj instanceof Request request)) {
                    System.out.println("Received non-request object from client: " + socket);
                    safeSendResponse(new ErrorResponse("Invalid request object"));
                    break;
                }

                System.out.println("Received request: " + request.getClass().getSimpleName());

                Response response;
                try {
                    response = auctionController.handleRequest(request, this);

                    if (response == null) {
                        response = new ErrorResponse("Server returned null response");
                    }
                } catch (Exception e) {
                    System.out.println("Unexpected server error while handling request");
                    e.printStackTrace();
                    response = new ErrorResponse("Internal server error");
                }

                safeSendResponse(response);
            }
        } catch (IOException e) {
            System.out.println("I/O error while handling client: " + socket);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Fatal error in client handler: " + socket);
            e.printStackTrace();
        } finally {
            sessionWatchRegistry.unwatchAll(this);
            closeResources();
        }
    }

    private synchronized boolean safeSendResponse(Response response) {
        if (out == null) {
            return false;
        }

        try {
            out.writeObject(response);
            out.flush();
            return true;
        } catch (IOException e) {
            System.out.println("Failed to send response to client: " + socket);
            return false;
        }
    }

    @Override
    public boolean onBidUpdated(BidUpdateResponse update) {
        if (update == null) {
            return false;
        }

        return safeSendResponse(update);
    }

    private void closeResources() {
        try {
            if (in != null) {
                in.close();
            }
        } catch (Exception ignored) {
        }

        try {
            if (out != null) {
                out.close();
            }
        } catch (Exception ignored) {
        }

        try {
            if (socket != null && !socket.isClosed()) {
                socket.close();
            }
        } catch (Exception ignored) {
        }

        System.out.println("Closed connection: " + socket);
    }
}