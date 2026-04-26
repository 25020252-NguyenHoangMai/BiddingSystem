package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.ItemDTO;
import com.auction.request.GetAllItemsRequest;
import com.auction.response.GetAllItemsResponse;

import java.util.List;

public class ProductService {

    private static final ProductService INSTANCE = new ProductService();
    private static final String GET_ALL_PRODUCTS = "GET_ALL_PRODUCTS";

    private ProductService() {}

    public static ProductService getInstance() {
        return INSTANCE;
    }

    public List<ItemDTO> getAllProducts() {
        try {
            ClientSocket socket = ClientSocket.getInstance();

            socket.sendRequest(GET_ALL_PRODUCTS);
            Object response = socket.receiveResponse();

            if (!(response instanceof List<?> list)) {
                throw new RuntimeException("Dữ liệu trả về không đúng định dạng danh sách");
            }

            return list.stream()
                    .filter(ItemDTO.class::isInstance)
                    .map(ItemDTO.class::cast)
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Không thể tải danh sách sản phẩm: " + e.getMessage(), e);
        }
    }
}
