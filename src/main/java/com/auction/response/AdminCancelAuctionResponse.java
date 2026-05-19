package com.auction.response;

import com.auction.dto.ItemDTO;

public class AdminCancelAuctionResponse extends Response {
    private final ItemDTO item;

    public AdminCancelAuctionResponse(boolean success, String message, ItemDTO item) {
        super(success, message);
        this.item = item;
    }

    public ItemDTO getItem() {
        return item;
    }
}
