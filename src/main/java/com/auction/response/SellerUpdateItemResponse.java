package com.auction.response;

import com.auction.dto.ItemDTO;

public class SellerUpdateItemResponse extends Response {
    private final ItemDTO itemDTO;

    public SellerUpdateItemResponse(boolean success, String message, ItemDTO itemDTO) {
        super(success, message);
        this.itemDTO = itemDTO;
    }

    public ItemDTO getItemDTO() {
        return itemDTO;
    }
}
