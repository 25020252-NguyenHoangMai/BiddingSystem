package com.auction.server.controller;

import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.dto.ItemDTO;
import com.auction.model.User;
import com.auction.request.AddItemRequest;
import com.auction.request.GetAllItemsRequest;
import com.auction.response.AddItemResponse;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.DashboardUpdateType;
import com.auction.response.GetAllItemsResponse;
import com.auction.server.factory.ItemFromDTOFactory;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.service.ItemService;
import com.auction.server.service.SessionService;
import com.auction.server.service.UserService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class ItemController {
    private final ItemService itemService;
    private final SessionService sessionService;
    private final UserService userService;
    private final DashboardWatchRegistry dashboardWatchRegistry;

    public ItemController(ItemService itemService, SessionService sessionService, UserService userService, DashboardWatchRegistry dashboardWatchRegistry) {
        this.itemService = itemService;
        this.sessionService = sessionService;
        this.userService = userService;
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

            // THÊM: tạo AuctionSession với endTimeMillis từ client
            Item item = ItemFromDTOFactory.createItem(createdItem);
            LocalDateTime now = LocalDateTime.now();

            long endMillis = request.getItem().getEndTimeMillis();
            LocalDateTime endTime;
            if (endMillis > 0) {
                endTime = Instant.ofEpochMilli(endMillis)
                        .atZone(ZoneId.systemDefault())
                        .toLocalDateTime();
            } else {
                endTime = now.plusHours(24); // fallback
            }

            if (!endTime.isAfter(now.plusMinutes(1))) {
                return new AddItemResponse(false, "Thời gian đấu giá quá ngắn hoặc đã hết hạn!", null);
            }

            // Tạo session, giữ lại object trả về
            AuctionSession session = sessionService.createSession(item, now, endTime);

            // Build fullDTO trực tiếp
            ItemDTO fullDTO = createdItem;
            fullDTO.setSessionId(session.getId());
            fullDTO.setSessionStatus(session.getStatus());       // "OPEN"
            fullDTO.setCurrentPrice(session.getCurrentPrice());  // = startingPrice
            fullDTO.setEndTimeMillis(                            // tính từ endTime server
                    endTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
            );
            try {
                User seller = userService.getUserById(request.getSellerId());
                fullDTO.setSellerUsername(seller.getUsername());
            } catch (Exception ignored) {}

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
