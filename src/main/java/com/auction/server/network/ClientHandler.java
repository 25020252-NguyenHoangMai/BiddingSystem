package com.auction.server.network;

import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import java.io.*;
import java.net.Socket;

public class ClientHandler extends Thread {
    private final Socket socket;

    public ClientHandler(Socket socket) {
        this.socket = socket;
    }

    @Override
    public void run() {
        try (ObjectInputStream in = new ObjectInputStream(socket.getInputStream());
             ObjectOutputStream out = new ObjectOutputStream(socket.getOutputStream())) {

            Object request = in.readObject();
            UserDAO dao = new UserDAO(); // Khai báo dùng chung cho cả class

            // Xử lý đăng kí
            if (request instanceof User) {
                User user = (User) request;
                System.out.println("Yêu cầu Đăng ký: " + user.getUsername());
                try {
                    dao.register(user);
                    out.writeObject("SUCCESS");
                } catch (Exception e) {
                    System.err.println("Lỗi Đăng ký: " + e.getMessage());
                    out.writeObject("FAIL");
                }
            }

            // Xử lý đăng nhập
            else if (request instanceof String[]) {
                String[] data = (String[]) request;
                String command = data[0]; // Chữ "LOGIN"

                if ("LOGIN".equals(command)) {
                    String username = data[1];
                    String password = data[2];
                    System.out.println("Yêu cầu Đăng nhập: " + username);

                    try {
                        // Gọi hàm authenticate trả về đối tượng User
                        User authenticatedUser = dao.authenticate(username, password);

                        if (authenticatedUser != null) {
                            out.writeObject("SUCCESS");
                        } else {
                            out.writeObject("FAIL");
                        }
                    } catch (Exception e) {
                        // Bắt lỗi sai pass hoặc lỗi SQL từ UserDAO
                        System.err.println("Lỗi xác thực: " + e.getMessage());
                        out.writeObject("FAIL");
                    }
                }
            }

            out.flush();

        } catch (IOException | ClassNotFoundException e) {
            System.err.println("Lỗi kết nối: " + e.getMessage());
        } finally {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
