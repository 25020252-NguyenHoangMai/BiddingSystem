package com.auction.request;

import com.auction.dto.ItemDTO;

public class AddItemRequest extends Request {
    private final String sellerId;
    private final ItemDTO item;

    public AddItemRequest(String sellerId, ItemDTO item) {
        this.sellerId = sellerId;
        this.item = item;
    }

    public ItemDTO getItem() {
        return item;
    }

    public String getSellerId() {
        return sellerId;
    }
}
