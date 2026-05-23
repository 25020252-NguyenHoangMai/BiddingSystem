package com.auction.server.factory;

import com.auction.dto.ItemDTO;
import com.auction.model.Item;
import com.auction.model.OtherItem;

public class OtherItemFactory implements ItemFactory {

    @Override
    public Item createItem(ItemDTO dto) {
        OtherItem item = new OtherItem();


        item.setId(dto.getId());
        item.setName(dto.getName());
        item.setDescription(dto.getDescription());
        item.setStartingPrice(dto.getStartingPrice());
        item.setSellerId(dto.getSellerId());
        item.setImagePath(dto.getImagePath());
        item.setItemType("OTHER");

        return item;
    }
}