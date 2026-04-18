package com.auction.server;

import com.auction.server.network.ClientHandler;

import java.io.IOException;
import java.net.ServerSocket;
import java.net.Socket;

public class MainServer {
    public static void main(String[] args) {
        int port = 1234;
        try (ServerSocket serverSocket = new ServerSocket(port)) {
            System.out.println("Server đấu giá đang chạy tại cổng " + port);

            while (true) {
                // Chấp nhận kết nối từ Client
                Socket clientSocket = serverSocket.accept();
                System.out.println("Có một người dùng vừa kết nối!");

                // Tạo một luồng (Thread) riêng để xử lý người dùng này
                new ClientHandler(clientSocket).start();
            }
        } catch (IOException e) {
            System.err.println("Lỗi khởi động Server: " + e.getMessage());
        }
    }
}


