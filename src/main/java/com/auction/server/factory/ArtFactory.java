package com.auction.server.factory;

import com.auction.dto.ItemDTO;
import com.auction.model.Item;
import com.auction.model.Art;
public class ArtFactory implements ItemFactory {
    @Override
    public Item createItem(ItemDTO dto) {
        return new Art(dto.getId(), dto.getName(), dto.getSellerId(), dto.getArtist());
    }

}
