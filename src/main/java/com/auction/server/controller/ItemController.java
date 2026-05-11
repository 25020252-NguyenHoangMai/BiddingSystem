package com.auction.server.controller;

import com.auction.model.Item;
import com.auction.dto.ItemDTO;
import com.auction.request.AddItemRequest;
import com.auction.request.GetAllItemsRequest;
import com.auction.response.AddItemResponse;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.DashboardUpdateType;
import com.auction.response.GetAllItemsResponse;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.service.ItemService;
import com.auction.server.service.SessionService;
import com.auction.server.service.UserService;

import java.util.List;

public class ItemController {
    private final ItemService itemService;
    private final DashboardWatchRegistry dashboardWatchRegistry;

    public ItemController(ItemService itemService, DashboardWatchRegistry dashboardWatchRegistry) {
        this.itemService = itemService;
        this.dashboardWatchRegistry = dashboardWatchRegistry;
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

    public AddItemResponse addItem(AddItemRequest request) {
        try {
            if (request == null) {
                return new AddItemResponse(false, "Request can not be null", null);
            }

            ItemDTO createdItem = itemService.addItem(request.getSellerId(), request.getItem());

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Item added",
                            DashboardUpdateType.ITEM_ADDED,
                            createdItem
                    )
            );
            return new AddItemResponse(true, "Add item successfully", createdItem);

        } catch (Exception e) {
            e.printStackTrace();
            return new AddItemResponse(false, "Add item failed: " + e.getMessage(), null);
        }
    }
}
