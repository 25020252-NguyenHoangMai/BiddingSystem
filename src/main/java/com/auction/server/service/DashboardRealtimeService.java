package com.auction.server.service;

import com.auction.dto.ItemDTO;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.DashboardUpdateType;
import com.auction.server.realtime.DashboardWatchRegistry;

public class DashboardRealtimeService {
    private final DashboardWatchRegistry dashboardWatchRegistry;
    private final ItemService itemService;

    public DashboardRealtimeService(DashboardWatchRegistry dashboardWatchRegistry, ItemService itemService) {
        this.dashboardWatchRegistry = dashboardWatchRegistry;
        this.itemService = itemService;
    }

    public void broadcastItemAdded(ItemDTO itemDTO, String message) {
        broadcast(DashboardUpdateType.ITEM_ADDED, itemDTO, message);
    }

    // method dùng khi đã có sẵn full itemdto mới
    public void broadcastItemUpdated(ItemDTO item, String message) {
        broadcast(DashboardUpdateType.ITEM_UPDATED, item, message);
    }

    // dùng khi dto hiện có không đủ field
    public void broadcastItemUpdatedBySessionId(String sessionId, String message) {
        ItemDTO item = itemService.getAuctionDetailDTO(sessionId);
        broadcastItemUpdated(item, message);
    }

    public void broadcastSellerItemsUpdated(String sellerId, String message) {
        if (sellerId == null || sellerId.isBlank()) {
            return;
        }

        for (ItemDTO item : itemService.getDashboardItemDTOSBySellerId(sellerId)) {
            broadcastItemUpdated(item, message);
        }
    }

    public void broadcastItemRemoved(ItemDTO item, String message) {
        broadcast(DashboardUpdateType.ITEM_REMOVED, item, message);
    }

    private void broadcast(DashboardUpdateType type, ItemDTO item, String message) {
        if (item == null) {
            return;
        }

        dashboardWatchRegistry.broadcastDashboardUpdate(
                new DashboardUpdateResponse(
                        true,
                        message,
                        type,
                        item
                )
        );
    }
}
