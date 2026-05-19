package com.auction.request;

import com.auction.dto.ItemDTO;

public class SellerUpdateItemRequest extends Request {
    private final String sellerId;
    private final ItemDTO item;

    public SellerUpdateItemRequest(String sellerId, ItemDTO item) {
        this.sellerId = sellerId;
        this.item = item;
    }

    public String getSellerId() {
        return sellerId;
    }

    public ItemDTO getItem() {
        return item;
    }
}
