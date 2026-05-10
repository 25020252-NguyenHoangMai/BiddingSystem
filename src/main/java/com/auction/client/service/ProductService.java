package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.ItemDTO;
import com.auction.request.AddItemRequest;
import com.auction.request.GetAllItemsRequest;
import com.auction.response.AddItemResponse;
import com.auction.response.ErrorResponse;
import com.auction.response.GetAllItemsResponse;
import com.auction.response.Response;

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

            if (response instanceof ErrorResponse err) {
                throw new RuntimeException(err.getMessage());
            }

            if (response instanceof Response res) {
                throw new RuntimeException(res.getMessage());
            }

            if (response == null) {
                throw new RuntimeException("Server không phản hồi hoặc bị timeout");
            }

            throw new Exception("Định dạng phản hồi từ Server không đúng: " + response.getClass().getName());

        } catch (Exception e) {
            throw new RuntimeException("Không thể tải danh sách sản phẩm: " + e.getMessage());
        }
    }

    public ItemDTO addProduct(ItemDTO item) {
        try {
            // 1. Lấy kết nối Socket
            ClientSocket socket = ClientSocket.getInstance();
            socket.connect();

            // 2. Gửi gói tin AddItemRequest chứa đối tượng item
            AddItemRequest request = new AddItemRequest(item.getSellerId(), item);
            socket.sendRequest(request);

            // 3. Nhận phản hồi từ Server
            Object response = socket.receiveResponse();

            // 4. Kiểm tra xem Server trả về đúng kiểu AddItemResponse không
            if (response instanceof AddItemResponse res) {
                if (res.isSuccess()) {
                    // Trả về item đã lưu (đã có ID từ Server)
                    return res.getItemDTO();
                } else {
                    // Nếu Server báo lỗi (ví dụ: dữ liệu không hợp lệ), ném lỗi với message từ Server
                    throw new RuntimeException(res.getMessage());
                }
            }

            throw new Exception("Định dạng phản hồi từ Server không đúng (Expected AddItemResponse)");

        } catch (Exception e) {
            // Log lỗi và ném ra RuntimeException để UI xử lý
            e.printStackTrace();
            throw new RuntimeException("Lỗi thêm sản phẩm: " + e.getMessage());
        }
    }
}
