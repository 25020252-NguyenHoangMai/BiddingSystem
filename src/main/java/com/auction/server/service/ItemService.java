package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.server.dao.ItemDAO;
import com.auction.dto.ItemDTO;
import com.auction.server.factory.ItemFromDTOFactory;


import java.util.List;
import java.util.stream.Collectors;


public class ItemService {
    private ItemDAO itemDAO ;

    public ItemService() {
        this.itemDAO = new ItemDAO();
    }

    //=============== thêm item đấu giá ===============
    public void addItem(Item item) {
        if (item.getStartingPrice() < 0) {
            throw new AuctionException("Starting price cannot be negative!");
        }
        else if (item.getName() == null || item.getName().trim().isEmpty()) {
            throw new AuctionException("Product name cannot be empty!");
        }

        //tạo id ngẫu nhiên
        String id = java.util.UUID.randomUUID().toString();
        item.setId(id);

        itemDAO.insertItem(item);
    }

    //=============== cập nhật thông tin sản phẩm ===============
    public void updateItem(Item item) {
        ItemDTO existing = itemDAO.getItemById(item.getId());
        if (existing == null) {
            throw new ItemNotFoundException("Item is not found!");
        }
        itemDAO.updateItem(item);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems()
                .stream()
                .map(ItemFromDTOFactory::createItem)
                .collect(Collectors.toList());
    }


    //=============== hiển thị sản phẩm (qua id) ===============
    public Item getItemById(String id) {
        ItemDTO dto = itemDAO.getItemById(id);

        if (dto == null) {
            throw new ItemNotFoundException("Item is not found!");
        }
        return ItemFromDTOFactory.createItem(dto);
    }


    //=============== hiển thị sản phẩm (qua tên) ===============
    public List<Item> getItemByName(String name) {
        List<ItemDTO> list = itemDAO.getItemByName(name);

        if (list.isEmpty()) {
            throw new ItemNotFoundException("Item is not found!");
        }
        return list.stream()
                .map(ItemFromDTOFactory::createItem)
                .collect(Collectors.toList());
    }


    //=============== hiển thị sản phẩm (qua type) ===============
    public List<Item> getItemByItemType(String itemType) {
        List<ItemDTO> list = itemDAO.getItemByItemType(itemType);

        if (list.isEmpty()) {
            throw new ItemNotFoundException("Item is not found!");
        }
        return list.stream()
                .map(ItemFromDTOFactory::createItem)
                .collect(Collectors.toList());
    }


    //=============== xóa sản phẩm ===============
    public void removeItem(String id) {
        ItemDTO dto = itemDAO.getItemById(id);

        if (dto == null) {
            throw new ItemNotFoundException("Item is not found!");
        }
        itemDAO.deleteItem(id);
    }
}
