package com.auction.response;

import com.auction.model.Item;
import java.util.List;

public class GetAllItemsResponse extends Response {
    private List<Item> items;

    public GetAllItemsResponse(boolean success, String message, List<Item> items) {
        super(success, message);
        this.items = items;
    }

    public List<Item> getItems() {
        return items;
    }
}
