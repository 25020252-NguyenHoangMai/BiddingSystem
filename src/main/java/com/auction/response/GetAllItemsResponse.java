package com.auction.response;

import com.auction.dto.ItemDTO;
import java.util.List;

public class GetAllItemsResponse extends Response {
    private List<ItemDTO> items;

    public GetAllItemsResponse(boolean success, String message, List<ItemDTO> items) {
        super(success, message);
        this.items = items;
    }

    public List<ItemDTO> getItems() {
        return items;
    }
}
