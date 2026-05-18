package com.auction.server.network;

import com.auction.protocol.BaseMessage;
import com.auction.protocol.EventMessage;
import com.auction.protocol.RequestMessage;
import com.auction.protocol.ResponseMessage;
import com.auction.request.Request;
import com.auction.response.BidUpdateResponse;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.Response;
import com.auction.response.ErrorResponse;
import com.auction.server.controller.AuctionController;
import com.auction.server.realtime.AuctionSessionObserver;
import com.auction.server.realtime.DashboardObserver;
import com.auction.server.realtime.DashboardWatchRegistry;
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

public class ClientHandler implements Runnable, AuctionSessionObserver, DashboardObserver {
    private final Socket socket;
    private final AuctionController auctionController;
    private final SessionWatchRegistry sessionWatchRegistry;
    private final DashboardWatchRegistry dashboardWatchRegistry;

    private ObjectInputStream in;
    private ObjectOutputStream out;

    public ClientHandler(Socket socket, AuctionController auctionController, SessionWatchRegistry sessionWatchRegistry,
                         DashboardWatchRegistry dashboardWatchRegistry) {
        this.socket = socket;
        this.auctionController = auctionController;
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.dashboardWatchRegistry = dashboardWatchRegistry;
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
                    safeSendMessage(new ResponseMessage("unknown",
                            new ErrorResponse("Corrupted request stream")));
                    break;
                } catch (InvalidClassException e) {
                    System.out.println("Invalid class sent by client: " + socket);
                    safeSendMessage(new ResponseMessage("unknown",
                            new ErrorResponse("Invalid request class")));
                    break;
                } catch (OptionalDataException e) {
                    System.out.println("Unexpected raw data from client: " + socket);
                    safeSendMessage(new ResponseMessage("unknown",
                            new ErrorResponse("Unexpected data format")));
                    break;
                } catch (ClassNotFoundException e) {
                    System.out.println("Unknown class from client: " + socket);
                    safeSendMessage(new ResponseMessage("unknown", new ErrorResponse("Unknown request type")));
                    break;
                }

                if (!(obj instanceof RequestMessage requestMessage)) {
                    System.out.println("Received non-request protocol message from client: " + socket);
                    safeSendMessage(new ResponseMessage("unknown",
                            new ErrorResponse("Invalid request message")));
                    break;
                }

                String requestId = requestMessage.getRequestId();
                if (requestId == null || requestId.isBlank()) {
                    System.out.println("Received request without requestId from client: " + socket);
                    safeSendMessage(new ResponseMessage("unknown", new ErrorResponse("Missing requestId")));
                    break;
                }

                Object payload = requestMessage.getPayload();
                if (!(payload instanceof Request request)) {
                    System.out.println("Received invalid payload from client: " + socket);
                    safeSendMessage(new ResponseMessage(requestId, new ErrorResponse("Invalid request payload")));
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

                safeSendMessage(new ResponseMessage(requestId, response));
            }
        } catch (IOException e) {
            System.out.println("I/O error while handling client: " + socket);
            e.printStackTrace();
        } catch (Exception e) {
            System.out.println("Fatal error in client handler: " + socket);
            e.printStackTrace();
        } finally {
            sessionWatchRegistry.unwatchAll(this);
            dashboardWatchRegistry.unwatchAll(this);
            closeResources();
        }
    }

    private synchronized boolean safeSendMessage(BaseMessage message) {
        if (out == null) {
            return false;
        }

        try {
            out.writeObject(message);
            out.flush();
            return true;
        } catch (IOException e) {
            System.out.println("Failed to send message to client: " + socket);
            return false;
        }
    }

    @Override
    public boolean onBidUpdated(BidUpdateResponse update) {
        if (update == null) {
            return false;
        }

        return safeSendMessage(new EventMessage(update));
    }

    @Override
    public boolean onDashboardUpdate(DashboardUpdateResponse update) {
        if (update == null) {
            return false;
        }

        return safeSendMessage(new EventMessage(update));
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