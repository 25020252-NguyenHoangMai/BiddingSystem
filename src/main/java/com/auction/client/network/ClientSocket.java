package com.auction.client.network;

import java.io.*;
import java.net.Socket;

public class ClientSocket {
    public Object sendRequest(Object request) {
        try (Socket socket = new Socket("localhost", 1234);
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream());
             ObjectInputStream in = new ObjectInputStream(socket.getInputStream())) {

            out.writeObject(request); // Gửi User đi
            out.flush();
            return in.readObject(); // Đợi Server trả lời "SUCCESS"

        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }
}
