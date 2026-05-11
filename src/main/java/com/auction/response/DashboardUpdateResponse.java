package com.auction.response;

import com.auction.dto.ItemDTO;

public class DashboardUpdateResponse extends Response {
    private final ItemDTO item;
    private final DashboardUpdateType type;

    public DashboardUpdateResponse(boolean success, String message, DashboardUpdateType type, ItemDTO item) {
        super(success, message);
        this.type = type;
        this.item = item;
    }

    public ItemDTO getItem() {
        return item;
    }

    public DashboardUpdateType getType() {
        return type;
    }
}
