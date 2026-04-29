package com.auction.request;

import com.auction.dto.ItemDTO;

public class AddItemRequest extends Request {
    private final ItemDTO item;

    public AddItemRequest(ItemDTO item) {
        this.item = item;
    }

    public ItemDTO getItem() {
        return item;
    }
}
