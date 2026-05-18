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
        ClientSocket socket = ClientSocket.getInstance();
        try {
            GetAllItemsResponse response =
                    socket.sendRequestAndWait(new GetAllItemsRequest(), GetAllItemsResponse.class);

            if (!response.isSuccess()) {
                throw new RuntimeException(response.getMessage());
            }

            List<ItemDTO> items = response.getItems();

            return items != null
                    ? items
                    : new ArrayList<>();

        } catch (Exception e) {
            System.err.println("[ProductService] getAllProducts failed: " + e.getMessage());
            e.printStackTrace();
            throw new RuntimeException("Không thể tải danh sách sản phẩm: " + e.getMessage(), e);
        }
    }

    public ItemDTO addProduct(ItemDTO item) {
        ClientSocket socket = ClientSocket.getInstance();
        try {
            // 1. Lấy kết nối Socket
            socket.connect();

            AddItemRequest request = new AddItemRequest(item.getSellerId(), item, item.getDurationHours());

            AddItemResponse response =
                    socket.sendRequestAndWait(
                            request,
                            AddItemResponse.class
                    );

            if (!response.isSuccess()) {
                throw new RuntimeException(response.getMessage());
            }

            ItemDTO savedItem = response.getItemDTO();

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
