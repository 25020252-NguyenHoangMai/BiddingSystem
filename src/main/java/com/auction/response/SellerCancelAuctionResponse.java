package com.auction.response;

import com.auction.dto.ItemDTO;

public class SellerCancelAuctionResponse extends Response {
    private final ItemDTO itemDTO;

    public SellerCancelAuctionResponse(boolean success, String messsage, ItemDTO itemDTO) {
        super(success, messsage);
        this.itemDTO = itemDTO;
    }

    public ItemDTO getItemDTO() {
        return itemDTO;
    }

}
