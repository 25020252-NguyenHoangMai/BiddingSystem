package com.auction.client.service;

import com.auction.client.network.ClientSocket;
import com.auction.dto.ItemDTO;

import java.util.List;

public class ProductService {

    public List<ItemDTO> getAllProducts() {
        ClientSocket socket = ClientSocket.getInstance();

        socket.sendRequest("GET_ALL_PRODUCTS");
        Object res = socket.receiveResponse();

        if (!(res instanceof List<?> rawList)) {
            throw new RuntimeException("Invalid response");
        }

        return rawList.stream()
                .filter(ItemDTO.class::isInstance)
                .map(ItemDTO.class::cast)
                .toList();
    }

    private static ProductService instance;
    public static ProductService getInstance() {
        if (instance == null) instance = new ProductService();
        return instance;
    }
}