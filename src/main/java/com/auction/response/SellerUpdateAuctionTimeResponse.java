package com.auction.response;

import com.auction.dto.ItemDTO;

public class SellerUpdateAuctionTimeResponse extends Response {
    private final ItemDTO itemDTO;

    public SellerUpdateAuctionTimeResponse(boolean success, String message, ItemDTO itemDTO) {
        super(success, message);
        this.itemDTO = itemDTO;
    }

    public ItemDTO getItemDTO() {
        return itemDTO;
    }
}
