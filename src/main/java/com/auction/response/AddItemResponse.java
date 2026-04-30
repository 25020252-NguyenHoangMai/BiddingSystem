package com.auction.response;

import com.auction.dto.ItemDTO;

public class AddItemResponse extends Response {
    private final ItemDTO itemDTO;

    public AddItemResponse(boolean success, String message, ItemDTO itemDTO) {
        super(success, message);
        this.itemDTO = itemDTO;
    }

    public ItemDTO getItemDTO() {
        return itemDTO;
    }
}
