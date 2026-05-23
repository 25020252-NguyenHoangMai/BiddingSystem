package com.auction.server.service;

import com.auction.dto.SellerHistoryItemDTO;
import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.ItemDAO;
import com.auction.dto.ItemDTO;
import com.auction.server.dao.SessionDAO;
import com.auction.server.factory.ItemFromDTOFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.SQLException;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.stream.Collectors;


public class ItemService {
    private static final String SESSION_ID_PREFIX = "SS-";
    private static final String SESSION_ID_CHARS = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789";
    private static final int SESSION_ID_LENGTH = 8;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Logger log = LoggerFactory.getLogger(ItemService.class);

    private final ItemDAO itemDAO;
    private final UserService userService;
    private final SessionDAO sessionDAO;
    private final BidDAO bidDAO;

    public ItemService(ItemDAO itemDAO, UserService userService, SessionDAO sessionDAO, BidDAO bidDAO) {
        if (itemDAO == null) {
            throw new IllegalArgumentException("ItemDAO must not be null");
        }
        if (userService == null) {
            throw new IllegalArgumentException("UserService must not be null");
        }
        if (sessionDAO == null) {
            throw new IllegalArgumentException("SessionDAO must not be null");
        }
        if (bidDAO == null) {
            throw new IllegalArgumentException("BidDAO must not be null");
        }

        this.itemDAO = itemDAO;
        this.userService = userService;
        this.sessionDAO = sessionDAO;
        this.bidDAO = bidDAO;
    }

    //=============== thêm item đấu giá ===============
    public void addItem(String sellerId, Item item) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new AuctionException("SellerId must not be null!");
        }

        User user = userService.requireActiveUserById(sellerId);

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

        userService.requireActiveUserById(sellerId);

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

                AuctionSession session = findOpenSession(sessions);

                if (session == null) {
                    throw new AuctionException("Only auctions that have not started can update item details.");
                }

                if (hasBid(conn, session)) {
                    throw new AuctionException("Cannot update auction after it has bids.");
                }

                ItemDTO result = updateOpenAuctionItem(conn, existingItem, itemDTO, session);

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

        if (itemDTO.getImagePath() == null || itemDTO.getImagePath().isBlank()) {
            itemDTO.setImagePath(existingItem.getImagePath());
        }

        Item item = ItemFromDTOFactory.createItem(itemDTO);

        itemDAO.updateItem(conn, item);
        sessionDAO.updateSchedule(conn, session.getId(), startTime, endTime);
        // Đồng bộ currentPrice theo startingPrice mới nếu chưa có ai bid
        sessionDAO.updateStartingPrice(conn, session.getId(), item.getStartingPrice());

        session.setStartTime(startTime);
        session.setEndTime(endTime);
        session.setCurrentPrice(item.getStartingPrice());

        ItemDTO updatedItem = itemDAO.getItemById(conn, itemDTO.getId());

        if (updatedItem == null) {
            throw new ItemNotFoundException("Updated item is not found.");
        }

        return buildFullItemDTO(updatedItem, session);
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

    private boolean hasBid(Connection conn, AuctionSession session) {
        boolean hasWinner = session.getCurrentWinnerId() != null && !session.getCurrentWinnerId().isBlank();

        return hasWinner || bidDAO.existsBidBySessionId(conn, session.getId());
    }

    private LocalDateTime toLocalDateTime(long millis, String fieldName) {
        if (millis <= 0) {
            throw new AuctionException(fieldName + " is required.");
        }

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    private AuctionSession findOpenSession(List<AuctionSession> sessions) {
        AuctionSession openSession = null;

        for (AuctionSession session : sessions) {
            if (isEditableOpenSession(session)) {
                if (openSession != null) {
                    throw new AuctionException("Item has more than one OPEN auction session.");
                }

                openSession = session;
            }
        }

        return openSession;
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

    public ItemDTO addItemWithSession(String sellerId, ItemDTO itemDTO, LocalDateTime startTime,
                                      LocalDateTime endTime) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new AuctionException("Seller id is required");
        }
        if (itemDTO == null) {
            throw new AuctionException("ItemDTO must not be null");
        }
        if (startTime == null || endTime == null) {
            throw new AuctionException("Start and end time must not be null");
        }
        if (!endTime.isAfter(startTime)) {
            throw new AuctionException("End time must be after start time");
        }

        User user = userService.requireActiveUserById(sellerId);
        if (!(user instanceof Bidder bidder)) {
            throw new AuctionException("Only bidder accounts can enable seller features.");
        }

        if (!bidder.isSellerEnabled()) {
            throw new AuctionException("This bidder account has not enabled seller mode.");
        }

        Item item = ItemFromDTOFactory.createItem(itemDTO);

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
                if (sessionDAO.existsActiveSessionByItemId(conn, item.getId())) {
                    throw new AuctionException("This item already has an OPEN or RUNNING auction session.");
                }

                itemDAO.insertItem(conn, item);

                String sessionId = generateSessionId(conn);
                AuctionSession session = new AuctionSession(sessionId, item, startTime, endTime);

                sessionDAO.insertSession(conn, session, item);

                ItemDTO createdItem = itemDAO.getItemById(conn, item.getId());

                if (createdItem == null) {
                    throw new ItemNotFoundException("Created item is not found.");
                }

                ItemDTO fullDTO = buildFullItemDTO(createdItem, session);

                conn.commit();

                return fullDTO;

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

            throw new AuctionException("Add item with auction session failed: " + e.getMessage());
        }

    }

    private String generateSessionId(Connection conn) {
        for (int attempt = 0; attempt < 10; attempt++) {
            String sessionId = SESSION_ID_PREFIX + randomCode(SESSION_ID_LENGTH);

            if (sessionDAO.getSessionById(conn, sessionId) == null) {
                return sessionId;
            }
        }

        throw new AuctionException("Cannot generate unique session id.");
    }

    private String randomCode(int length) {
        StringBuilder code = new StringBuilder(length);

        for (int i = 0; i < length; i++) {
            int index = RANDOM.nextInt(SESSION_ID_CHARS.length());
            code.append(SESSION_ID_CHARS.charAt(index));
        }

        return code.toString();
    }

    public List<SellerHistoryItemDTO> getSellerHistory(String sellerId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new AuctionException("Seller id is required.");
        }

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            return sessionDAO.getSessionHistoryBySeller(conn, sellerId);
        } catch (SQLException e) {
            throw new AuctionException("Get seller history failed: " + e.getMessage());
        }
    }

    private boolean isEditableOpenSession(AuctionSession session) {
        if (session == null || !SessionService.STATUS_OPEN.equals(session.getStatus())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();

        if (session.getStartTime() == null || session.getEndTime() == null) {
            return false;
        }

        return now.isBefore(session.getStartTime()) && session.getEndTime().isAfter(session.getStartTime());
    }

}
