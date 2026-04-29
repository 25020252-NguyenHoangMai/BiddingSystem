package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.ItemDTO;
import com.auction.request.AddItemRequest;
import com.auction.request.GetAllItemsRequest;
import com.auction.response.AddItemResponse;
import com.auction.response.GetAllItemsResponse;

import java.util.ArrayList;
import java.util.List;

public class ProductService {

    private static final ProductService INSTANCE = new ProductService();

    private ProductService() {}

    public static ProductService getInstance() {
        return INSTANCE;
    }

    public List<ItemDTO> getAllProducts() {
        try {
            ClientSocket socket = ClientSocket.getInstance();
            socket.connect();

            GetAllItemsRequest request = new GetAllItemsRequest();
            socket.sendRequest(request);

            Object response = socket.receiveResponse();

            if (response instanceof GetAllItemsResponse itemResponse) {
                if (itemResponse.isSuccess()) {
                    // Trả về danh sách nếu thành công (không null)
                    return itemResponse.getItems() != null ? itemResponse.getItems() : new ArrayList<>();
                } else {
                    // Quăng lỗi với message từ Server
                    throw new RuntimeException(itemResponse.getMessage());
                }
            }

            throw new Exception("Định dạng phản hồi từ Server không đúng"); // Trường hợp Server gặp lỗi trả về String

        } catch (Exception e) {
            throw new RuntimeException("Không thể tải danh sách sản phẩm: " + e.getMessage());
        }
    }

    public boolean addProduct(ItemDTO item) {
        try {
            // 1. Lấy kết nối Socket
            ClientSocket socket = ClientSocket.getInstance();
            socket.connect();

            // 2. Gửi gói tin AddItemRequest chứa đối tượng item
            AddItemRequest request = new AddItemRequest(item);
            socket.sendRequest(request);

            // 3. Nhận phản hồi từ Server
            Object response = socket.receiveResponse();

            // 4. Kiểm tra xem Server trả về đúng kiểu AddItemResponse không
            if (response instanceof AddItemResponse res) {
                // Trả về true hoặc false tùy vào kết quả xử lý của Server
                return res.isSuccess();
            }

            // Trường hợp Server trả về sai kiểu gói tin
            System.err.println("Phản hồi từ Server không hợp lệ.");
            return false;

        } catch (Exception e) {
            // Trường hợp bị mất mạng hoặc Server sập
            e.printStackTrace();
            return false;
        }
    }
}
