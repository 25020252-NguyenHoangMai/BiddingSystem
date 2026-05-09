package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.UserSessionDTO;
import com.auction.request.EnableSellerRequest;
import com.auction.response.EnableSellerResponse;

import java.util.List;

public class UserClientService {

    // Áp dụng Singleton để AdminController dễ gọi
    private static UserClientService instance;

    private UserClientService() {}

    public static UserClientService getInstance() {
        if (instance == null) {
            instance = new UserClientService();
        }
        return instance;
    }

    // Lấy toàn bộ danh sách người dùng từ Server
    public List<UserSessionDTO> getAllUsers() {
        ClientSocket socket = ClientSocket.getInstance();

        // 1. Gửi yêu cầu lên Server
        socket.sendRequest("GET_ALL_USERS");

        // 2. Nhận phản hồi
        Object res = socket.receiveResponse();

        // 3. Kiểm tra và ép kiểu an toàn
        if (res instanceof List<?> rawList) {
            return rawList.stream()
                    .filter(UserSessionDTO.class::isInstance)
                    .map(UserSessionDTO.class::cast)
                    .toList();
        }

        return List.of(); // Trả về danh sách rỗng nếu có lỗi
    }

    // Gửi yêu cầu xóa người dùng theo ID
    public boolean deleteUser(int userId) {
        ClientSocket socket = ClientSocket.getInstance();

        // Gửi lệnh kèm ID (Ví dụ định dạng: DELETE_USER:123)
        socket.sendRequest("DELETE_USER:" + userId);

        Object res = socket.receiveResponse();
        return res instanceof Boolean && (Boolean) res;
    }

    // Gửi EnableSellerRequest lên Server
    public boolean enableSeller(String userId) throws Exception {
        ClientSocket socket = ClientSocket.getInstance();
        socket.connect();

        socket.sendRequest(new EnableSellerRequest(userId));
        Object raw = socket.receiveResponse();

        if (raw instanceof EnableSellerResponse res) {
            if (res.isSuccess()) return true;
            throw new Exception(res.getMessage());
        }
        throw new Exception("Phản hồi từ server không hợp lệ");
    }
}
