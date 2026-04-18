package com.auction.client.network;

import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.net.Socket;
public class ClientSocket {
    private static ClientSocket instance;

    private Socket socket;
    private ObjectOutputStream out;
    private ObjectInputStream in;

    private ClientSocket() {}

    public static ClientSocket getInstance() {
        if (instance == null) {
            instance = new ClientSocket();
        }
        return instance;
    }

    public void connect() {
        try {
            if (socket != null && socket.isConnected() && !socket.isClosed()) {
                return;
            }
            socket = new Socket("127.0.0.1", 5000);

            out = new ObjectOutputStream(socket.getOutputStream());
            in = new ObjectInputStream(socket.getInputStream());

            System.out.println("Connected to server");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void sendRequest(Object request) {
        try {
            out.writeObject(request);
            out.flush();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Object receiveResponse() {
        try {
            return in.readObject();
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public void close() {
        try {
            if (in != null) in.close();
            if (out != null) out.close();
            if (socket != null) socket.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
