package com.auction.server.controller;

import com.auction.dto.SellerHistoryItemDTO;
import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.dto.ItemDTO;
import com.auction.model.User;
import com.auction.request.*;
import com.auction.response.*;
import com.auction.server.factory.ItemFromDTOFactory;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.*;

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
    private final DashboardRealtimeService dashboardRealtimeService;
    private final AuctionDetailRealtimeService auctionDetailRealtimeService;
    private final ImageStorageService imageStorageService;

    public ItemController(ItemService itemService, SessionService sessionService,
                          DashboardRealtimeService dashboardRealtimeService,
                          AuctionDetailRealtimeService auctionDetailRealtimeService,
                          ImageStorageService imageStorageService) {
        this.itemService = itemService;
        this.sessionService = sessionService;
        this.dashboardRealtimeService = dashboardRealtimeService;
        this.auctionDetailRealtimeService = auctionDetailRealtimeService;
        this.imageStorageService = imageStorageService;
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

            String imagePath = imageStorageService.saveItemImage(
                    request.getImageBytes(),
                    request.getImageFileName()
            );

            requestItem.setImagePath(imagePath);

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

            dashboardRealtimeService.broadcastItemAdded(fullDTO, "Item added");
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

            dashboardRealtimeService.broadcastItemRemoved(dto, "Auction canceled by admin");

            auctionDetailRealtimeService.broadcastAuctionCanceled(session,
                    AuctionDetailRealtimeService.EVENT_AUCTION_CANCELED_BY_ADMIN);

            return new AdminCancelAuctionResponse(true, "Auction canceled successfully", dto);

        } catch (Exception e) {
            e.printStackTrace();
            return new AdminCancelAuctionResponse(false, "Cancel auction failed: " + e.getMessage(), null);
        }
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

            dashboardRealtimeService.broadcastItemRemoved(dto, "Auction canceled by seller");

            auctionDetailRealtimeService.broadcastAuctionCanceled(session,
                    AuctionDetailRealtimeService.EVENT_AUCTION_CANCELED_BY_SELLER);

            return new SellerCancelAuctionResponse(true, "Auction canceled successfully", dto);

        } catch (Exception e) {
            e.printStackTrace();
            return new SellerCancelAuctionResponse(false, "Cancel auction failed: " + e.getMessage(), null);
        }
    }

//Seller updates auction details for OPEN sessions, or RUNNING sessions without bids.

    // RUNNING updates keep the original start time.
    public SellerUpdateItemResponse sellerUpdateItem(SellerUpdateItemRequest request) {
        try {
            if (request == null) {
                return new SellerUpdateItemResponse(false, "Request cannot be null", null);
            }
            if (request.getItem() == null) {
                return new SellerUpdateItemResponse(false, "Item cannot be null", null);
            }

            ItemDTO itemDTO = request.getItem();
            //ko tin imagepath từ client
            itemDTO.setImagePath(null);

            byte[] imageBytes = itemDTO.getImageBytes();
            String imageFileName = itemDTO.getImageFileName();

            boolean hasNewImage = imageBytes != null && imageBytes.length > 0;

            if (hasNewImage) {
                if (imageFileName == null || imageFileName.isBlank()) {
                    return new SellerUpdateItemResponse(
                            false,
                            "Image file name is required when uploading a new image",
                            null
                    );
                }

                try {
                    String newImagePath = imageStorageService.saveItemImage(
                            imageBytes,
                            imageFileName
                    );

                    itemDTO.setImagePath(newImagePath);

                } catch (Exception e) {
                    return new SellerUpdateItemResponse(
                            false,
                            "Image upload failed: " + e.getMessage(),
                            null
                    );
                }
            }

            ItemDTO updatedItem = itemService.updateItemBySeller(
                    request.getSellerId(),
                    itemDTO
            );

            dashboardRealtimeService.broadcastItemUpdated(updatedItem, "Item updated by seller");

            auctionDetailRealtimeService.broadcastItemUpdated(updatedItem,
                    AuctionDetailRealtimeService.EVENT_ITEM_UPDATED_BY_SELLER);

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

            dashboardRealtimeService.broadcastItemUpdated(dto, "Auction details updated by seller");

            auctionDetailRealtimeService.broadcastItemUpdated(dto,
                    AuctionDetailRealtimeService.EVENT_AUCTION_END_TIME_UPDATED_BY_SELLER);

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

    public GetItemImageResponse getItemImage(GetItemImageRequest request) {
        try {
            if (request == null) {
                return new GetItemImageResponse(false, "Request cannot be null", null);
            }

            byte[] imageBytes = imageStorageService.readImage(request.getImagePath());

            if (imageBytes == null) {
                return new GetItemImageResponse(false, "Image not found", null);
            }

            return new GetItemImageResponse(true, "Get image successfully", imageBytes);

        } catch (Exception e) {
            e.printStackTrace();
            return new GetItemImageResponse(false, "Get image failed: " + e.getMessage(), null);
        }
    }

    public GetSellerHistoryResponse getSellerHistory(GetSellerHistoryRequest request) {
        try {
            if (request == null || request.getUserId() == null || request.getUserId().isBlank()) {
                return new GetSellerHistoryResponse(false, "Seller id is required", List.of());
            }

            List<SellerHistoryItemDTO> sessions = itemService.getSellerHistory(request.getUserId());

            return new GetSellerHistoryResponse(true, "Get seller history successfully", sessions);

        } catch (Exception e) {
            e.printStackTrace();
            return new GetSellerHistoryResponse(false, "Get seller history failed: " + e.getMessage(), List.of());
        }
    }
}
