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
        ClientSocket socket = new ClientSocket();
        try {
            socket.connect();

            GetAllItemsRequest request = new GetAllItemsRequest();
            socket.sendRequest(request);

            Object response = socket.receiveResponse();

            if (response instanceof GetAllItemsResponse itemResponse) {
                if (!itemResponse.isSuccess()) {
                    throw new RuntimeException(itemResponse.getMessage());
                }

                List<ItemDTO> items = itemResponse.getItems();

                return items != null
                        ? items
                        : new ArrayList<>();
            }

            if (response instanceof ErrorResponse err) {
                throw new RuntimeException(err.getMessage());
            }

            if (response instanceof Response res) {
                throw new RuntimeException(res.getMessage());
            }

            throw new IllegalStateException("Expected GetAllItemsResponse but got: "
                            + (response == null
                            ? "null"
                            : response.getClass().getSimpleName())
            );

        } catch (Exception e) {
            System.err.println("[ProductService] getAllProducts failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể tải danh sách sản phẩm: " + e.getMessage(), e);
        }
    }

    public ItemDTO addProduct(ItemDTO item) {
        ClientSocket socket = new ClientSocket();
        try {
            // 1. Lấy kết nối Socket
            socket.connect();

            // 2. Gửi gói tin AddItemRequest chứa đối tượng item
            AddItemRequest request = new AddItemRequest(item.getSellerId(), item, item.getDurationHours());
            socket.sendRequest(request);

            // 3. Nhận phản hồi từ Server
            Object response = socket.receiveResponse();

            // 4. Kiểm tra xem Server trả về đúng kiểu AddItemResponse không
            if (!(response instanceof AddItemResponse res)) {
                throw new IllegalStateException("Expected AddItemResponse but got: "
                                + (response == null
                                ? "null"
                                : response.getClass().getSimpleName())
                );
            }

            if (!res.isSuccess()) {
                throw new RuntimeException(res.getMessage());
            }

            ItemDTO savedItem = res.getItemDTO();

            if (savedItem == null) {
                throw new IllegalStateException("AddItemResponse itemDTO is null");
            }

            return savedItem;

        } catch (Exception e) {
            System.err.println("[ProductService] addProduct failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Lỗi thêm sản phẩm: " + e.getMessage(), e);
        }
    }
}
