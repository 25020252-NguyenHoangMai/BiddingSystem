package com.auction.server.service;

import com.auction.exception.ItemNotFoundException;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.server.dao.ItemDAO;
import com.auction.server.dto.ItemDTO;
import com.auction.server.factory.ItemFactory;


import java.util.List;
import java.util.stream.Collectors;


public class ItemService {
    private final ItemDAO itemDAO = new ItemDAO();

    //=============== thêm item đấu giá ===============
    public void addItem(Item item, User user) {

        //tạo id ngẫu nhiên
        String id = java.util.UUID.randomUUID().toString();
        item.setId(id);

        //thiết lập chủ sở hữu cho item
        item.setSellerID(user.getId());

        itemDAO.addItem(item);
    }

    //=============== cập nhật thông tin sản phẩm ===============
    public void updateItemInfo(Item item) {
        ItemDTO existing = itemDAO.getItemById(item.getId());
        if (existing == null) {
            throw new ItemNotFoundException("Không tồn tại!");
        }
        itemDAO.updateItemInfo(item);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems()
                .stream()
                .map(ItemFactory::createItem)
                .collect(Collectors.toList());
    }


    //=============== hiển thị sản phẩm (qua id) ===============
    public Item getItemById(String id) {
        ItemDTO dto = itemDAO.getItemById(id);

        if (dto == null) {
            throw new ItemNotFoundException("Sản phẩm không tồn tại!");
        }
        return ItemFactory.createItem(dto);
    }


    //=============== hiển thị sản phẩm (qua tên) ===============
    public List<Item> getItemByName(String name) {
        List<ItemDTO> list = itemDAO.getItemByName(name);

        if (list.isEmpty()) {
            throw new ItemNotFoundException("Sản phẩm không tồn tại!");
        }
        return list.stream()
                .map(ItemFactory::createItem)
                .collect(Collectors.toList());
    }


    //=============== hiển thị sản phẩm (qua type) ===============
    public List<Item> getItemByItemType(String itemType) {
        List<ItemDTO> list = itemDAO.getItemByItemType(itemType);

        if (list.isEmpty()) {
            throw new ItemNotFoundException("Sản phẩm không tồn tại!");
        }
        return list.stream()
                .map(ItemFactory::createItem)
                .collect(Collectors.toList());
    }


    //=============== xóa sản phẩm ===============
    public void deleteItem(String id) {
        ItemDTO dto = itemDAO.getItemById(id);

        if (dto == null) {
            throw new ItemNotFoundException("Sản phẩm không tồn tại!");
        }
        itemDAO.deleteItem(id);
    }
}
