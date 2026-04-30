package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.server.dao.ItemDAO;
import com.auction.dto.ItemDTO;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;
import com.auction.server.factory.ItemFromDTOFactory;


import java.util.List;
import java.util.stream.Collectors;


public class ItemService {
    private final ItemDAO itemDAO;
    private final UserService userService;
    private final SessionDAO sessionDAO;

    public ItemService(ItemDAO itemDAO, UserService userService, SessionDAO sessionDAO) {
        if (itemDAO == null) {
            throw new IllegalArgumentException("ItemDAO must not be null");
        }

        if (userService == null) {
            throw new IllegalArgumentException("UserService must not be null");
        }

        if (sessionDAO == null) {
            throw new IllegalArgumentException("SessionDAO must not be null");
        }
        this.itemDAO = itemDAO;
        this.userService = userService;
        this.sessionDAO = sessionDAO;
    }

    //=============== thêm item đấu giá ===============
    public void addItem(String sellerId, Item item) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new AuctionException("SellerId must not be null!");
        }

        User user = userService.getUserById(sellerId);

        if (!(user instanceof Bidder bidder)) {
            throw new AuctionException("Only bidder accounts can enable seller features.");
        }

        if (!bidder.isSellerEnabled()) {
            throw new AuctionException("This bidder account has not enabled seller mode.");
        }

        if (item == null) {
            throw new AuctionException("Item must not be null!");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new AuctionException("Product name cannot be empty!");
        }

        if (item.getStartingPrice() < 0) {
            throw new AuctionException("Starting price cannot be negative!");
        }

        item.setSellerId(sellerId);
        //tạo id ngẫu nhiên
        String id = java.util.UUID.randomUUID().toString();
        item.setId(id);

        itemDAO.insertItem(item);
    }

    public ItemDTO addItem(String sellerId, ItemDTO itemDTO) {
        if (itemDTO == null) {
            throw new AuctionException("ItemDTO must not be null!");
        }

        Item item = ItemFromDTOFactory.createItem(itemDTO);
        addItem(sellerId, item);

        itemDTO.setId(item.getId());
        itemDTO.setSellerId(item.getSellerId());
        itemDTO.setItemType(item.getClass().getSimpleName().toUpperCase());

        return itemDTO;
    }

    //=============== cập nhật thông tin sản phẩm ===============
    public void updateItem(Item item) {
        if (item == null) {
            throw new AuctionException("Item must not be null!");
        }

        if (item.getId() == null || item.getId().isBlank()) {
            throw new AuctionException("Item id is required!");
        }

        if (item.getName() == null || item.getName().isBlank()) {
            throw new AuctionException("Product name cannot be empty!");
        }

        if (item.getStartingPrice() < 0) {
            throw new AuctionException("Starting price cannot be negative!");
        }

        ItemDTO existingItem = itemDAO.getItemById(item.getId());
        if (existingItem == null) {
            throw new ItemNotFoundException("Item is not found!");
        }

        List<AuctionSession> sessions = sessionDAO.getSessionsByItemId(item.getId());
        for (AuctionSession session : sessions) {
            String status = session.getStatus();
            if (SessionService.STATUS_RUNNING.equals(status)
                || SessionService.STATUS_FINISHED.equals(status)
                || SessionService.STATUS_CANCELED.equals(status)) {
                throw new AuctionException("Cannot update item because it is already used in a running or completed auction session.");
            }
        }
        itemDAO.updateItem(item);
    }

    public List<Item> getAllItems() {
        return itemDAO.getAllItems()
                .stream()
                .map(ItemFromDTOFactory::createItem)
                .collect(Collectors.toList());
    }

    public List<ItemDTO> getAllItemDTOS() {
        return itemDAO.getAllItems();
    }

    //=============== hiển thị sản phẩm (qua id) ===============
    public Item getItemById(String id) {
        if (id == null || id.isBlank()) {
            throw new AuctionException("Item id is required!");
        }

        ItemDTO dto = itemDAO.getItemById(id);

        if (dto == null) {
            throw new ItemNotFoundException("Item is not found!");
        }
        return ItemFromDTOFactory.createItem(dto);
    }


    //=============== hiển thị sản phẩm (qua tên) ===============
    public List<Item> getItemByName(String name) {
        if (name == null || name.isBlank()) {
            throw new AuctionException("Item name is required!");
        }

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
        if (itemType == null || itemType.isBlank()) {
            throw new AuctionException("Item type is required!");
        }

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
        if (id == null || id.isBlank()) {
            throw new AuctionException("Item id is required!");
        }

        ItemDTO existingItem = itemDAO.getItemById(id);

        if (existingItem == null) {
            throw new ItemNotFoundException("Item is not found!");
        }

        if (sessionDAO.existsSessionByItemId(id)) {
            throw new AuctionException("Cannot remove item because it has already been used in an auction session.");
        }

        itemDAO.deleteItem(id);
    }
}
