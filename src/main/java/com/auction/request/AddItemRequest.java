package com.auction.request;

import com.auction.dto.ItemDTO;

public class AddItemRequest extends Request {
    private final String sellerId;
    private final ItemDTO item;
    private final int durationHours;

    public AddItemRequest(String sellerId, ItemDTO item, int durationHours) {
        this.sellerId = sellerId;
        this.item = item;
        this.durationHours = durationHours;
    }

    public ItemDTO getItem() {
        return item;
    }

    public String getSellerId() {
        return sellerId;
    }

    public int getDurationHours() {
        return  durationHours;
    }
}
