package com.auction.server.factory;


import com.auction.dto.ItemDTO;
import com.auction.model.Item;
import com.auction.model.Jewelry;

public class JewelryFactory implements ItemFactory {
    @Override
    public Item createItem(ItemDTO dto){
        return new Jewelry(
                dto.getId(),
                dto.getName(),
                dto.getDescription(),
                dto.getImagePath(),
                dto.getSellerId(),
                dto.getStartingPrice(),
                dto.getMaterial(),
                dto.getGemstone(),
                dto.getWeight(),
                dto.getBrand()
        );
    }
}
