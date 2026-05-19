package com.auction.server.controller;

import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.dto.ItemDTO;
import com.auction.model.User;
import com.auction.request.*;
import com.auction.response.*;
import com.auction.server.factory.ItemFromDTOFactory;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.ItemService;
import com.auction.server.service.SessionService;
import com.auction.server.service.UserService;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

public class ItemController {
    private static final String EVENT_ITEM_UPDATED_BY_SELLER = "ITEM_UPDATED_BY_SELLER";
    private static final String EVENT_AUCTION_END_TIME_UPDATED_BY_SELLER = "AUCTION_END_TIME_UPDATED_BY_SELLER";
    private static final String EVENT_AUCTION_CANCELED_BY_SELLER = "AUCTION_CANCELED_BY_SELLER";
    private static final String EVENT_AUCTION_CANCELED_BY_ADMIN = "AUCTION_CANCELED_BY_ADMIN";

    private final ItemService itemService;
    private final SessionService sessionService;
    private final DashboardWatchRegistry dashboardWatchRegistry;
    private final SessionWatchRegistry sessionWatchRegistry;

    public ItemController(ItemService itemService, SessionService sessionService,
                          DashboardWatchRegistry dashboardWatchRegistry, SessionWatchRegistry sessionWatchRegistry) {
        this.itemService = itemService;
        this.sessionService = sessionService;
        this.dashboardWatchRegistry = dashboardWatchRegistry;
        this.sessionWatchRegistry = sessionWatchRegistry;
    }

    public GetAllItemsResponse getAllItems(GetAllItemsRequest request) {
        try {
            if (request == null) {
                return new GetAllItemsResponse(false, "Request cannot be null", null);
            }

            List<ItemDTO> items = itemService.getAllItemDTOS();

            return new GetAllItemsResponse(true, "Get all items successfully", items);

        } catch (Exception e) {
            e.printStackTrace();
            return new GetAllItemsResponse(false, "Get all items failed: " + e.getMessage(), null);
        }
    }

    public AddItemResponse addItem(AddItemRequest request) {
        try {
            if (request == null) {
                return new AddItemResponse(false, "Request can not be null", null);
            }
            if (request.getItem() == null) {
                return new AddItemResponse(false, "Item can not be null", null);
            }

            ItemDTO requestItem = request.getItem();

            LocalDateTime startTime = toLocalDateTime(requestItem.getStartTimeMillis(), "Auction start time");

            LocalDateTime endTime = toLocalDateTime(requestItem.getEndTimeMillis(), "Auction end time");

            if (!endTime.isAfter(startTime)) {
                return new AddItemResponse(false, "Auction end time must be after start time.", null);
            }

            LocalDateTime now = LocalDateTime.now();

            if (startTime.isBefore(now.minusMinutes(1))) {
                return new AddItemResponse(false, "Auction start time cannot be in the past.", null);
            }

            ItemDTO fullDTO = itemService.addItemWithSession(
                    request.getSellerId(),
                    requestItem,
                    startTime,
                    endTime
            );

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Item added",
                            DashboardUpdateType.ITEM_ADDED,
                            fullDTO
                    )
            );
            return new AddItemResponse(true, "Add item successfully", fullDTO);

        } catch (Exception e) {
            e.printStackTrace();
            return new AddItemResponse(false, "Add item failed: " + e.getMessage(), null);
        }
    }

    public GetAuctionDetailResponse getAuctionDetail(GetAuctionDetailRequest request) {
        try {
            if (request == null) {
                return new GetAuctionDetailResponse(false, "Request cannot be null", null);
            }

            ItemDTO item = itemService.getAuctionDetailDTO(request.getSessionId());

            return new GetAuctionDetailResponse(true, "Get auction detail successfully", item);

        } catch (Exception e) {
            return new GetAuctionDetailResponse(false, "Get auction detail failed: " + e.getMessage(), null);
        }
    }

    public AdminCancelAuctionResponse adminCancelAuction(AdminCancelAuctionRequest request) {
        try {
            if (request == null) {
                return new AdminCancelAuctionResponse(false, "Request cannot be null", null);
            }

            AuctionSession session = sessionService.cancelSessionByAdmin(request.getAdminId(),
                                    request.getSessionId());

            ItemDTO dto = itemService.getAuctionDetailDTO(session.getId());

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Auction canceled by admin",
                            DashboardUpdateType.ITEM_REMOVED,
                            dto
                    )
            );

            broadcastAuctionCanceledToSessionWatchers(session, EVENT_AUCTION_CANCELED_BY_ADMIN);

            return new AdminCancelAuctionResponse(true, "Auction canceled successfully", dto);

        } catch (Exception e) {
            e.printStackTrace();
            return new AdminCancelAuctionResponse(false, "Cancel auction failed: " + e.getMessage(), null);
        }
    }

    private void broadcastAuctionCanceledToSessionWatchers(AuctionSession session, String message) {
        if (session == null) {
            return;
        }

        Long endTimeMillis = null;
        if (session.getEndTime() != null) {
            endTimeMillis = session.getEndTime()
                    .atZone(ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        BidUpdateResponse update = new BidUpdateResponse(
                true,
                message,
                session.getId(),
                session.getCurrentPrice(),
                session.getCurrentWinnerId(),
                null,
                SessionService.STATUS_CANCELED,
                endTimeMillis,
                null,
                null,
                null
        );

        sessionWatchRegistry.broadcastBidUpdate(session.getId(), update);
    }

    public SellerCancelAuctionResponse sellerCancelAuction(SellerCancelAuctionRequest request) {
        try {
            if (request == null) {
                return new SellerCancelAuctionResponse(false, "Request cannot be null",
                                            null);
            }

            AuctionSession session = sessionService.cancelSessionBySeller(request.getSellerId(),
                                        request.getSessionId());

            ItemDTO dto = itemService.getAuctionDetailDTO(session.getId());

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Auction canceled by seller",
                            DashboardUpdateType.ITEM_REMOVED,
                            dto
                    )
            );

            broadcastAuctionCanceledToSessionWatchers(session, EVENT_AUCTION_CANCELED_BY_SELLER);

            return new SellerCancelAuctionResponse(true, "Auction canceled successfully", dto);

        } catch (Exception e) {
            e.printStackTrace();
            return new SellerCancelAuctionResponse(false, "Cancel auction failed: " + e.getMessage(), null);
        }
    }

//method dùng khi session open
    public SellerUpdateItemResponse sellerUpdateItem(SellerUpdateItemRequest request) {
        try {
            if (request == null) {
                return new SellerUpdateItemResponse(false, "Request cannot be null", null);
            }
            if (request.getItem() == null) {
                return new SellerUpdateItemResponse(false, "Item cannot be null", null);
            }

            ItemDTO updatedItem = itemService.updateItemBySeller(
                    request.getSellerId(),
                    request.getItem()
            );

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Item updated by seller",
                            DashboardUpdateType.ITEM_UPDATED,
                            updatedItem
                    )
            );

            broadcastAuctionUpdatedToSessionWatchers(updatedItem, EVENT_ITEM_UPDATED_BY_SELLER);

            return new SellerUpdateItemResponse(true, "Item updated successfully", updatedItem);

        } catch (Exception e) {
            e.printStackTrace();
            return new SellerUpdateItemResponse(false, "Update item failed: " + e.getMessage(),
                                    null);
        }
    }

    private LocalDateTime toLocalDateTime(long millis, String fieldName) {
        if (millis <= 0) {
            throw new IllegalArgumentException(fieldName + " is required.");
        }

        return Instant.ofEpochMilli(millis)
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
    }

    public SellerUpdateAuctionTimeResponse sellerUpdateAuctionTime(SellerUpdateAuctionTimeRequest request) {
        try {
            if (request == null) {
                return new SellerUpdateAuctionTimeResponse(false, "Request cannot be null", null);
            }

            LocalDateTime endTime = toLocalDateTime(
                    request.getEndTimeMillis(),
                    "Auction end time"
            );

            AuctionSession session = sessionService.updateEndTimeBySeller(
                    request.getSellerId(),
                    request.getSessionId(),
                    endTime
            );

            ItemDTO dto = itemService.getAuctionDetailDTO(session.getId());

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Auction end time updated by seller",
                            DashboardUpdateType.ITEM_UPDATED,
                            dto
                    )
            );

            broadcastAuctionUpdatedToSessionWatchers(dto, EVENT_AUCTION_END_TIME_UPDATED_BY_SELLER);

            return new SellerUpdateAuctionTimeResponse(
                    true,
                    "Auction end time updated successfully",
                    dto
            );

        } catch (Exception e) {
            e.printStackTrace();
            return new SellerUpdateAuctionTimeResponse(
                    false,
                    "Update auction end time failed: " + e.getMessage(),
                    null
            );
        }
    }

    private void broadcastAuctionUpdatedToSessionWatchers(ItemDTO item, String message) {
        if (item == null || item.getSessionId() == null || item.getSessionId().isBlank()) {
            return;
        }

        Long endTimeMillis = item.getEndTimeMillis() > 0
                ? item.getEndTimeMillis()
                : null;

        BidUpdateResponse update = new BidUpdateResponse(
                true,
                message,
                item.getSessionId(),
                item.getCurrentPrice(),
                null,
                item.getCurrentWinnerUsername(),
                item.getSessionStatus(),
                endTimeMillis,
                item.getMinimumNextBid(),
                null,
                null
        );

        sessionWatchRegistry.broadcastBidUpdate(item.getSessionId(), update);
    }
}
