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

            ItemDTO createdItem = itemService.addItem(request.getSellerId(), request.getItem());

            // THÊM: tạo AuctionSession với endTimeMillis từ client
            Item item = ItemFromDTOFactory.createItem(createdItem);
            LocalDateTime now = LocalDateTime.now();

            int durationHours = request.getDurationHours();

            if (durationHours < 1 || durationHours > 720) {
                return new AddItemResponse(false, "Auction duration must be between 1 and 720 hours!",
                                    null);
            }

            LocalDateTime endTime = now.plusHours(durationHours);

            // Tạo session, giữ lại object trả về
            AuctionSession session = sessionService.createSession(item, now, endTime);

            ItemDTO fullDTO = itemService.buildFullItemDTO(createdItem, session);

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

            AuctionSession session = sessionService.cancelSessionByAdmin(request.getAdminId(), request.getSessionId());

            ItemDTO dto = itemService.getAuctionDetailDTO(session.getId());

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Auction canceled by admin",
                            DashboardUpdateType.ITEM_UPDATED,
                            dto
                    )
            );

            broadcastAuctionCanceledToSessionWatchers(session, "Auction canceled by admin");

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
                return new SellerCancelAuctionResponse(false, "Request cannot be null", null);
            }

            AuctionSession session = sessionService.cancelSessionBySeller(request.getSellerId(), request.getSessionId());

            ItemDTO dto = itemService.getAuctionDetailDTO(session.getId());

            dashboardWatchRegistry.broadcastDashboardUpdate(
                    new DashboardUpdateResponse(
                            true,
                            "Auction canceled by seller",
                            DashboardUpdateType.ITEM_REMOVED,
                            dto
                    )
            );

            broadcastAuctionCanceledToSessionWatchers(session, "Auction canceled by seller");

            return new SellerCancelAuctionResponse(true, "Auction canceled successfully", dto);

        } catch (Exception e) {
            e.printStackTrace();
            return new SellerCancelAuctionResponse(false, "Cancel auction failed: " + e.getMessage(), null);
        }
    }

    public SellerUpdateItemResponse sellerUpdateItem(SellerUpdateItemRequest request) {
        try {
            if (request == null) {
                return new SellerUpdateItemResponse(false, "Request cannot be null", null);
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

            return new SellerUpdateItemResponse(true, "Item updated successfully", updatedItem);

        } catch (Exception e) {
            e.printStackTrace();
            return new SellerUpdateItemResponse(false, "Update item failed: " + e.getMessage(),
                                    null);
        }
    }
}
