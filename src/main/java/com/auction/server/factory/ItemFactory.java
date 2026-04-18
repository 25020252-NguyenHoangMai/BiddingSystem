package com.auction.server.factory;

import com.auction.model.*;
import com.auction.server.dto.ItemDTO;

public class ItemFactory {
    public static Item createItem(ItemDTO data) {
        switch (data.itemType == null ? "" : data.itemType.toUpperCase()) {
            case "VEHICLE":
                return new Vehicle(data.id, data.name, data.description, data.sellerID, data.startingPrice, data.model, data.engineType, data.mileage);
            case "ELECTRONICS":
                return new Electronics(data.id, data.name, data.description, data.sellerID, data.startingPrice, data.brand);
            case "ART":
                return new Art(data.id, data.name, data.description, data.sellerID, data.startingPrice, data.artist);
            default:
                throw new RuntimeException("ItemType không hợp lệ: " + data.itemType);
        }
    }
}
