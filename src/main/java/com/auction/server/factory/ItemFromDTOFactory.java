package com.auction.server.factory;

import com.auction.model.Item;
import com.auction.dto.ItemDTO;

public class ItemFromDTOFactory {
    public static Item createItem(ItemDTO data) {
        String itemType = data.getItemType() == null ? "" : data.getItemType().toUpperCase();
        ItemFactory factory = ItemFactoryRegistry.getFactory(itemType);
        if (factory == null) {
            throw new RuntimeException("ItemType không hợp lệ hoặc chưa đăng ký factory: " + data.getItemType());
        }
        return factory.createItem(data);
    }
}