package com.auction.server.service;

import com.auction.dto.ItemDTO;
import com.auction.model.AuctionSession;
import com.auction.response.BidUpdateResponse;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.SessionDAO;
import com.auction.server.realtime.SessionWatchRegistry;

import java.sql.Connection;
import java.time.ZoneId;
import java.util.HashSet;
import java.util.Set;

public class AuctionDetailRealtimeService {
    public static final String EVENT_ITEM_UPDATED_BY_SELLER = "ITEM_UPDATED_BY_SELLER";
    public static final String EVENT_AUCTION_END_TIME_UPDATED_BY_SELLER = "AUCTION_END_TIME_UPDATED_BY_SELLER";
    public static final String EVENT_AUCTION_CANCELED_BY_SELLER = "AUCTION_CANCELED_BY_SELLER";
    public static final String EVENT_AUCTION_CANCELED_BY_ADMIN = "AUCTION_CANCELED_BY_ADMIN";
    public static final String EVENT_USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";

    private final SessionWatchRegistry sessionWatchRegistry;
    private final SessionDAO sessionDAO;
    private final BidDAO bidDAO;
    private final ItemService itemService;

    public AuctionDetailRealtimeService(SessionWatchRegistry sessionWatchRegistry, SessionDAO sessionDAO,
                                        BidDAO bidDAO, ItemService itemService) {
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.sessionDAO = sessionDAO;
        this.bidDAO = bidDAO;
        this.itemService = itemService;
    }

    public void broadcastItemUpdated(ItemDTO item, String message) {
        if (item == null || item.getSessionId() == null || item.getSessionId().isBlank()) {
            return;
        }

        BidUpdateResponse update = new BidUpdateResponse(
                true,
                message,
                item.getSessionId(),
                item.getCurrentPrice(),
                null,
                item.getCurrentWinnerUsername(),
                item.getSessionStatus(),
                item.getEndTimeMillis() > 0 ? item.getEndTimeMillis() : null,
                item.getMinimumNextBid(),
                null,
                null
        );

        sessionWatchRegistry.broadcastBidUpdate(item.getSessionId(), update);
    }

    public void broadcastAuctionCanceled(AuctionSession session, String message) {
        if (session == null || session.getId() == null || session.getId().isBlank()) {
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
                session.getStatus(),
                endTimeMillis,
                null,
                null,
                null
        );

        sessionWatchRegistry.broadcastBidUpdate(session.getId(), update);
    }

    public void broadcastUsernameChanged(String userId) {
        if (userId == null || userId.isBlank()) {
            return;
        }

        Set<String> sessionIds = new HashSet<>();

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            sessionIds.addAll(sessionDAO.getVisibleSessionIdsBySellerId(conn, userId));
            sessionIds.addAll(bidDAO.getVisibleSessionIdsByBidderId(conn, userId));
        } catch (Exception e) {
            e.printStackTrace();
            return;
        }

        for (String sessionId : sessionIds) {
            try {
                ItemDTO item = itemService.getAuctionDetailDTO(sessionId);
                broadcastItemUpdated(item, EVENT_USER_PROFILE_UPDATED);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
}
