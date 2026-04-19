package com.auction.server.factory;

import com.auction.model.*;
import com.auction.server.dto.ItemDTO;

public class ItemFactory {
    public static Item createItem(ItemDTO data) {
        return switch (data.getItemType() == null ? "" : data.getItemType().toUpperCase()) {
            case "VEHICLE" ->
                    new Vehicle(data.getId(), data.getName(), data.getDescription(), data.getSellerId(), data.getStartingPrice(), data.getModel(), data.getEngineType(), data.getMileage());
            case "ELECTRONICS" ->
                    new Electronics(data.getId(), data.getName(), data.getDescription(), data.getSellerId(), data.getStartingPrice(), data.getBrand());
            case "ART" ->
                    new Art(data.getId(), data.getName(), data.getDescription(), data.getSellerId(), data.getStartingPrice(), data.getArtist());
            default -> throw new RuntimeException("ItemType không hợp lệ: " + data.getItemType());
        };
    }
}
