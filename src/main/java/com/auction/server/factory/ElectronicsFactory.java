package com.auction.server.factory;

import com.auction.dto.ItemDTO;
import com.auction.model.Electronics;
import com.auction.model.Item;

public class ElectronicsFactory implements ItemFactory {
    @Override
    public Item createItem(ItemDTO dto) {
        return new Electronics(
                dto.getId(),
                dto.getName(),
                dto.getDescription(),
                dto.getSellerId(),
                dto.getStartingPrice(),
                dto.getBrand()
        );
    }

}
