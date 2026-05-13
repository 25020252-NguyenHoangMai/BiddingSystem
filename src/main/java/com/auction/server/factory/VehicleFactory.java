package com.auction.server.factory;

import com.auction.dto.ItemDTO;
import com.auction.model.Vehicle;
import com.auction.model.Item;

public class VehicleFactory implements ItemFactory {
    @Override
    public Item createItem(ItemDTO dto) {
        return new Vehicle (dto.getId(), dto.getName(), dto.getSellerId(),
                dto.getBrand(), dto.getEngineType(), dto.getMileage());
    }
}
