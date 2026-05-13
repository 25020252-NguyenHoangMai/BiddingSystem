package com.auction.server.factory;

import com.auction.dto.ItemDTO;
import com.auction.model.Item;

public interface ItemFactory {
    Item createItem(ItemDTO dto);
}
