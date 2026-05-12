package com.auction.response;

import com.auction.dto.ItemDTO;

public class GetAuctionDetailResponse extends Response {
    private final ItemDTO item;

    public GetAuctionDetailResponse(boolean success, String message, ItemDTO item) {
        super(success, message);
        this.item = item;
    }

    public ItemDTO getItem() {
        return item;
    }
}
