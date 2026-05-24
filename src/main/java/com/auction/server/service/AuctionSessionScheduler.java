package com.auction.server.service;

import com.auction.dto.ItemDTO;
import com.auction.model.AuctionSession;
import com.auction.response.BidUpdateResponse;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.DashboardUpdateType;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.realtime.SessionWatchRegistry;

import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionSessionScheduler {
    private final SessionService sessionService;
    private final SessionWatchRegistry  sessionWatchRegistry;
    private final DashboardWatchRegistry dashboardWatchRegistry;
    private final ItemService itemService;
    private final UserService userService;

    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "AuctionSessionScheduler");
                thread.setDaemon(true);
                return thread;
            });

    public AuctionSessionScheduler(SessionService sessionService, SessionWatchRegistry sessionWatchRegistry,
                                        DashboardWatchRegistry dashboardWatchRegistry, ItemService itemService,
                                        UserService userService) {
        this.sessionService = sessionService;
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.dashboardWatchRegistry = dashboardWatchRegistry;
        this.itemService = itemService;
        this.userService = userService;
    }

    public void start() {
        executor.scheduleAtFixedRate(
                this::runOnce,
                0,
                5,
                TimeUnit.SECONDS
        );
    }

    private void runOnce() {
        try {
            List<AuctionSession> startedSessions = sessionService.startDueSessions();
            if (startedSessions != null && !startedSessions.isEmpty()) {
                for (AuctionSession session : startedSessions) {
                    try {
                        broadcastSessionStarted(session);
                        broadcastDashboardUpdated(session);

                    } catch (Exception e) {
                        System.err.println("[AuctionSessionScheduler] Failed to broadcast started session: "
                                + session.getId());
                        e.printStackTrace();
                    }
                }
            }


            List<AuctionSession> finalizedSessions = sessionService.finalizeExpiredSessions();

            if (finalizedSessions == null || finalizedSessions.isEmpty()) {
                return;
            }
            for (AuctionSession auctionSession : finalizedSessions) {
                try {
                    broadcastSessionFinalized(auctionSession);
                    broadcastDashboardUpdated(auctionSession);
                } catch (Exception e) {
                    System.err.println("[AuctionSessionScheduler] Failed to broadcast finalized session: "
                                    + auctionSession.getId());
                    e.printStackTrace();
                }
            }
        } catch (Exception e) {
            System.err.println("[AuctionSessionScheduler] Failed to finalize expired sessions");
            e.printStackTrace();
        }
    }

    public void stop() {
        executor.shutdownNow();
    }

    private void broadcastSessionStarted(AuctionSession auctionSession) {
        Long endTimeMillis = null;

        if (auctionSession.getEndTime() != null) {
            endTimeMillis = auctionSession.getEndTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }
        BidUpdateResponse update = new BidUpdateResponse(
                true,
                "Auction started",
                auctionSession.getId(),
                auctionSession.getCurrentPrice(),
                auctionSession.getCurrentWinnerId(),
                null,
                auctionSession.getStatus(),
                endTimeMillis,
                null,
                null,
                null
        );

        sessionWatchRegistry.broadcastBidUpdate(auctionSession.getId(), update);

    }

    private void broadcastSessionFinalized(AuctionSession auctionSession) {
        String winnerUsername = null;

        if (auctionSession.getCurrentWinnerId() != null && !auctionSession.getCurrentWinnerId().isBlank()) {
            try {
                winnerUsername = userService.getUserById(auctionSession.getCurrentWinnerId()).getUsername();
            } catch (Exception ignored) {}
        }

        Long endTimeMillis = null;

        if (auctionSession.getEndTime() != null) {
            endTimeMillis = auctionSession.getEndTime()
                    .atZone(java.time.ZoneId.systemDefault())
                    .toInstant()
                    .toEpochMilli();
        }

        BidUpdateResponse update = new BidUpdateResponse(
                true,
                "Auction finalized",
                auctionSession.getId(),
                auctionSession.getCurrentPrice(),
                auctionSession.getCurrentWinnerId(),
                winnerUsername,
                auctionSession.getStatus(),
                endTimeMillis,
                null,
                null,
                null
        );

        sessionWatchRegistry.broadcastBidUpdate(auctionSession.getId(), update);
    }

    private void broadcastDashboardUpdated(AuctionSession auctionSession) {
        ItemDTO item = itemService.getAuctionDetailDTO(auctionSession.getId());

        DashboardUpdateResponse update = new DashboardUpdateResponse(
                true,
                "Auction finalized",
                DashboardUpdateType.ITEM_UPDATED,
                item
        );

        dashboardWatchRegistry.broadcastDashboardUpdate(update);
    }
}
