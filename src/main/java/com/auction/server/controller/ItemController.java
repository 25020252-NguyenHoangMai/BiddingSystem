package com.auction.server.controller;

import com.auction.model.Item;
import com.auction.dto.ItemDTO;
import com.auction.request.GetAllItemsRequest;
import com.auction.response.GetAllItemsResponse;
import com.auction.server.service.ItemService;
import java.util.List;

public class ItemController {
    private final ItemService itemService;

    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    public GetAllItemsResponse getAllItems(GetAllItemsRequest request) {
        try {
            if (request == null) {
                return new GetAllItemsResponse(false, "Request cannot be null", null);
            }

            List<ItemDTO> items = itemService.getAllItemDTOS();

            return new GetAllItemsResponse(true, "Get all items successfully", items);

        } catch (Exception e) {
            e.printStackTrace();
            return new GetAllItemsResponse(false, "Get all items failed: " + e.getMessage(), null);
        }
    }
}
