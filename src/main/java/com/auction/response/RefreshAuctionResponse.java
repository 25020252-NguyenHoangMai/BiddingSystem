package com.auction.response;

import com.auction.dto.ItemDTO;

import java.io.Serializable;

public class RefreshAuctionResponse implements Serializable {

    private final boolean success;
    private final ItemDTO item;

    public RefreshAuctionResponse(
            boolean success,
            ItemDTO item
    ) {
        this.success = success;
        this.item = item;
    }

    public boolean isSuccess() {
        return success;
    }

    public ItemDTO getItem() {
        return item;
    }
}
