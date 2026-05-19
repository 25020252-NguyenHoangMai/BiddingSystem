package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.ItemDAO;
import com.auction.dto.ItemDTO;
import com.auction.server.dao.SessionDAO;
import com.auction.server.factory.ItemFromDTOFactory;


import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Objects;
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
        item.setId(java.util.UUID.randomUUID().toString());

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                itemDAO.insertItem(conn, item);
                conn.commit();
            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            if (e instanceof AuctionException auctionException) {
                throw auctionException;
            }

            throw new AuctionException("Add item failed: " + e.getMessage());
        }
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

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                ItemDTO existingItem = itemDAO.getItemById(conn, item.getId());

                if (existingItem == null) {
                    throw new ItemNotFoundException("Item is not found!");
                }

                List<AuctionSession> sessions = sessionDAO.getSessionsByItemId(conn, item.getId());

                for (AuctionSession session : sessions) {
                    String status = session.getStatus();

                    if (SessionService.STATUS_RUNNING.equals(status)
                            || SessionService.STATUS_FINISHED.equals(status)
                            || SessionService.STATUS_PAID.equals(status)
                            || SessionService.STATUS_CANCELED.equals(status)) {
                        throw new AuctionException("Cannot update item because it is already used in a running or completed auction session.");
                    }
                }

                itemDAO.updateItem(conn, item);

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            if (e instanceof AuctionException auctionException) {
                throw auctionException;
            }

            throw new AuctionException("Update item failed: " + e.getMessage());
        }
    }

    public ItemDTO updateItemBySeller(String sellerId, ItemDTO itemDTO) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new AuctionException("Seller id is required.");
        }

        if (itemDTO == null) {
            throw new AuctionException("ItemDTO must not be null.");
        }

        if (itemDTO.getId() == null || itemDTO.getId().isBlank()) {
            throw new AuctionException("Item id is required.");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                ItemDTO existingItem = itemDAO.getItemById(conn, itemDTO.getId());

                if (existingItem == null) {
                    throw new ItemNotFoundException("Item is not found.");
                }

                if (existingItem.getSellerId() == null || !existingItem.getSellerId().equals(sellerId)) {
                    throw new AuctionException("You can only update your own item.");
                }

                List<AuctionSession> sessions =
                        sessionDAO.getSessionsByItemIdForUpdate(conn, itemDTO.getId());

                AuctionSession session = findEditableSession(sessions);

                if (session == null) {
                    throw new AuctionException("No editable auction session found for this item.");
                }

                if (hasBid(session)) {
                    throw new AuctionException("Cannot update auction after it has bids.");
                }

                ItemDTO result;

                if (SessionService.STATUS_OPEN.equals(session.getStatus())) {
                    result = updateOpenAuctionItem(conn, existingItem, itemDTO, session);
                } else if (SessionService.STATUS_RUNNING.equals(session.getStatus())) {
                    result = updateRunningAuctionEndTime(conn, existingItem, itemDTO, session);
                } else {
                    throw new AuctionException("Cannot update auction with status: " + session.getStatus());
                }

                conn.commit();
                return result;

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            if (e instanceof AuctionException auctionException) {
                throw auctionException;
            }

            throw new AuctionException("Update item failed: " + e.getMessage());
        }
    }

    private ItemDTO updateOpenAuctionItem(
            Connection conn,
            ItemDTO existingItem,
            ItemDTO itemDTO,
            AuctionSession session
    ) {
        validateItemFieldsForUpdate(itemDTO);

        LocalDateTime startTime = toLocalDateTime(
                itemDTO.getStartTimeMillis(),
                "Auction start time"
        );

        LocalDateTime endTime = toLocalDateTime(
                itemDTO.getEndTimeMillis(),
                "Auction end time"
        );

        validateOpenSchedule(startTime, endTime);

        itemDTO.setSellerId(existingItem.getSellerId());

        Item item = ItemFromDTOFactory.createItem(itemDTO);

        itemDAO.updateItem(conn, item);
        sessionDAO.updateSchedule(conn, session.getId(), startTime, endTime);

        session.setStartTime(startTime);
        session.setEndTime(endTime);

        ItemDTO updatedItem = itemDAO.getItemById(conn, itemDTO.getId());

        if (updatedItem == null) {
            throw new ItemNotFoundException("Updated item is not found.");
        }

        return buildFullItemDTO(updatedItem, session);
    }

    private ItemDTO updateRunningAuctionEndTime(
            Connection conn,
            ItemDTO existingItem,
            ItemDTO itemDTO,
            AuctionSession session
    ) {
        validateNoItemFieldChanged(existingItem, itemDTO);

        if (itemDTO.getStartTimeMillis() > 0) {
            LocalDateTime requestedStartTime = toLocalDateTime(
                    itemDTO.getStartTimeMillis(),
                    "Auction start time"
            );

            if (!requestedStartTime.equals(session.getStartTime())) {
                throw new AuctionException("Cannot update start time after auction has started.");
            }
        }

        LocalDateTime endTime = toLocalDateTime(
                itemDTO.getEndTimeMillis(),
                "Auction end time"
        );

        validateRunningEndTime(session, endTime);

        sessionDAO.updateEndTime(conn, session.getId(), endTime);
        session.setEndTime(endTime);

        return buildFullItemDTO(existingItem, session);
    }

    private void validateItemFieldsForUpdate(ItemDTO itemDTO) {
        if (itemDTO.getName() == null || itemDTO.getName().isBlank()) {
            throw new AuctionException("Product name cannot be empty.");
        }

        if (itemDTO.getItemType() == null || itemDTO.getItemType().isBlank()) {
            throw new AuctionException("Item type is required.");
        }

        if (itemDTO.getStartingPrice() <= 0) {
            throw new AuctionException("Starting price must be greater than 0.");
        }
    }

    private void validateOpenSchedule(LocalDateTime startTime, LocalDateTime endTime) {
        if (!endTime.isAfter(startTime)) {
            throw new AuctionException("Auction end time must be after start time.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (startTime.isBefore(now.minusMinutes(1))) {
            throw new AuctionException("Auction start time cannot be in the past.");
        }
    }

    private boolean hasBid(AuctionSession session) {
        return session.getCurrentWinnerId() != null
                && !session.getCurrentWinnerId().isBlank();
    }

    private LocalDateTime toLocalDateTime(long millis, String fieldName) {
        if (millis <= 0) {
            throw new AuctionException(fieldName + " is required.");
        }

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private AuctionSession findEditableSession(List<AuctionSession> sessions) {
        AuctionSession editableSession = null;

        for (AuctionSession session : sessions) {
            String status = session.getStatus();

            if (SessionService.STATUS_OPEN.equals(status)
                    || SessionService.STATUS_RUNNING.equals(status)) {
                if (editableSession != null) {
                    throw new AuctionException("Item has more than one editable auction session.");
                }

                editableSession = session;
            }
        }

        return editableSession;
    }

    private void validateNoItemFieldChanged(ItemDTO existingItem, ItemDTO requestedItem) {
        if (!Objects.equals(existingItem.getName(), requestedItem.getName())) {
            throw new AuctionException("Cannot update product name after auction has started.");
        }

        if (!Objects.equals(existingItem.getDescription(), requestedItem.getDescription())) {
            throw new AuctionException("Cannot update product description after auction has started.");
        }

        if (!Objects.equals(existingItem.getItemType(), requestedItem.getItemType())) {
            throw new AuctionException("Cannot update item type after auction has started.");
        }

        if (Double.compare(existingItem.getStartingPrice(), requestedItem.getStartingPrice()) != 0) {
            throw new AuctionException("Cannot update starting price after auction has started.");
        }

        if (!Objects.equals(existingItem.getModel(), requestedItem.getModel())) {
            throw new AuctionException("Cannot update vehicle model after auction has started.");
        }

        if (!Objects.equals(existingItem.getEngineType(), requestedItem.getEngineType())) {
            throw new AuctionException("Cannot update engine type after auction has started.");
        }

        if (existingItem.getMileage() != requestedItem.getMileage()) {
            throw new AuctionException("Cannot update mileage after auction has started.");
        }

        if (!Objects.equals(existingItem.getBrand(), requestedItem.getBrand())) {
            throw new AuctionException("Cannot update brand after auction has started.");
        }

        if (!Objects.equals(existingItem.getArtist(), requestedItem.getArtist())) {
            throw new AuctionException("Cannot update artist after auction has started.");
        }
    }

    private void validateRunningEndTime(AuctionSession session, LocalDateTime endTime) {
        LocalDateTime now = LocalDateTime.now();

        if (!endTime.isAfter(now)) {
            throw new AuctionException("Auction end time must be in the future.");
        }

        if (!endTime.isAfter(session.getStartTime())) {
            throw new AuctionException("Auction end time must be after start time.");
        }
    }

    public List<Item> getAllItems() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return itemDAO.getAllItems(conn)
                    .stream()
                    .map(ItemFromDTOFactory::createItem)
                    .collect(Collectors.toList());
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting all items: " + e.getMessage());
        }
    }

    public List<ItemDTO> getAllItemDTOS() {
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return itemDAO.getAllItemsForDashboard(conn);
        } catch (SQLException e) {
            throw new AuctionException("An error occurred while getting dashboard items: " + e.getMessage());
        }
    }

    //=============== hiển thị sản phẩm (qua id) ===============
    public Item getItemById(String id) {
        if (id == null || id.isBlank()) {
            throw new AuctionException("Item id is required!");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            ItemDTO dto = itemDAO.getItemById(conn, id);

            if (dto == null) {
                throw new ItemNotFoundException("Item is not found!");
            }

            return ItemFromDTOFactory.createItem(dto);

        } catch (SQLException e) {
            throw new AuctionException("Get item by id failed: " + e.getMessage());
        }
    }


    //=============== hiển thị sản phẩm (qua tên) ===============
    public List<Item> getItemByName(String name) {
        if (name == null || name.isBlank()) {
            throw new AuctionException("Item name is required!");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            List<ItemDTO> list = itemDAO.getItemByName(conn, name);

            if (list.isEmpty()) {
                throw new ItemNotFoundException("Item is not found!");
            }

            return list.stream()
                    .map(ItemFromDTOFactory::createItem)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            throw new AuctionException("Get item by name failed: " + e.getMessage());
        }
    }


    //=============== hiển thị sản phẩm (qua type) ===============
    public List<Item> getItemByItemType(String itemType) {
        if (itemType == null || itemType.isBlank()) {
            throw new AuctionException("Item type is required!");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            List<ItemDTO> list = itemDAO.getItemByItemType(conn, itemType);

            if (list.isEmpty()) {
                throw new ItemNotFoundException("Item is not found!");
            }

            return list.stream()
                    .map(ItemFromDTOFactory::createItem)
                    .collect(Collectors.toList());

        } catch (SQLException e) {
            throw new AuctionException("Get item by type failed: " + e.getMessage());
        }
    }


    //=============== xóa sản phẩm ===============
    public void removeItem(String id) {
        if (id == null || id.isBlank()) {
            throw new AuctionException("Item id is required!");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                ItemDTO existingItem = itemDAO.getItemById(conn, id);

                if (existingItem == null) {
                    throw new ItemNotFoundException("Item is not found!");
                }

                if (sessionDAO.existsSessionByItemId(conn, id)) {
                    throw new AuctionException("Cannot remove item because it has already been used in an auction session.");
                }

                itemDAO.deleteItem(conn, id);

                conn.commit();

            } catch (Exception e) {
                conn.rollback();
                throw e;
            } finally {
                conn.setAutoCommit(true);
            }

        } catch (Exception e) {
            if (e instanceof AuctionException auctionException) {
                throw auctionException;
            }

            throw new AuctionException("Remove item failed: " + e.getMessage());
        }
    }

    public ItemDTO getAuctionDetailDTO(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new AuctionException("Session id is required");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            AuctionSession session = sessionDAO.getSessionById(conn, sessionId);

            if (session == null) {
                throw new AuctionException("Auction session not found");
            }

            Item item = session.getItem();
            ItemDTO dto = itemDAO.getItemById(conn, item.getId());

            if (dto == null) {
                throw new ItemNotFoundException("Item is not found!");
            }

            return buildFullItemDTO(dto, session);

        } catch (SQLException e) {
            throw new AuctionException("Get auction detail failed: " + e.getMessage());
        }
    }

    public ItemDTO buildFullItemDTO(ItemDTO dto, AuctionSession session) {
        if (dto == null) {
            throw new AuctionException("ItemDTO must not be null");
        }

        if (session == null) {
            throw new AuctionException("Auction session must not be null");
        }

        dto.setSessionId(session.getId());
        dto.setSessionStatus(session.getStatus());
        dto.setCurrentPrice(session.getCurrentPrice());

        if (session.getStartTime() != null) {
            dto.setStartTimeMillis(
                    session.getStartTime()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );
        }

        if (session.getEndTime() != null) {
            dto.setEndTimeMillis(
                    session.getEndTime()
                            .atZone(ZoneId.systemDefault())
                            .toInstant()
                            .toEpochMilli()
            );
        }

        if (dto.getSellerId() != null && !dto.getSellerId().isBlank()) {
            User seller = userService.getUserById(dto.getSellerId());
            dto.setSellerUsername(seller.getUsername());
        }

        String winnerId = session.getCurrentWinnerId();
        if (winnerId != null && !winnerId.isBlank()) {
            User winner = userService.getUserById(winnerId);
            dto.setCurrentWinnerUsername(winner.getUsername());
        }

        return dto;
    }
}
