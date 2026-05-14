package com.auction.server.controller;

import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.User;
import com.auction.request.UnwatchDashboardRequest;
import com.auction.request.UnwatchSessionRequest;
import com.auction.request.WatchDashboardRequest;
import com.auction.request.WatchSessionRequest;
import com.auction.response.*;
import com.auction.server.realtime.AuctionSessionObserver;
import com.auction.server.realtime.DashboardObserver;
import com.auction.server.realtime.DashboardWatchRegistry;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.BidIncrementService;
import com.auction.server.service.SessionService;
import com.auction.server.service.UserService;

import java.time.ZoneId;

public class RealTimeController {
    private final SessionWatchRegistry sessionWatchRegistry;
    private final DashboardWatchRegistry dashboardWatchRegistry;
    private final SessionService sessionService;
    private final UserService userService;
    private final BidIncrementService bidIncrementService;

    public RealTimeController(SessionWatchRegistry sessionWatchRegistry,DashboardWatchRegistry dashboardWatchRegistry,
                              SessionService sessionService, UserService userService,
                              BidIncrementService bidIncrementService) {
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.dashboardWatchRegistry = dashboardWatchRegistry;
        this.sessionService = sessionService;
        this.userService = userService;
        this.bidIncrementService = bidIncrementService;
    }

    public Response watchSession(WatchSessionRequest request, AuctionSessionObserver observer) {
        try {
            if (observer == null) {
                return new SessionWatchResponse(false, "Observer is required");
            }
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new SessionWatchResponse(false, "Session ID is required");
            }
            if (request.getUserId() == null || request.getUserId().isBlank()) {
                return new SessionWatchResponse(false, "User ID is required");
            }

            String sessionId = request.getSessionId();
            AuctionSession session = sessionService.getSession(request.getSessionId());
            if (session == null) {
                return new SessionWatchResponse(false, "Session not found: " + sessionId);
            }

            boolean watched = sessionWatchRegistry.watchSession(sessionId, observer);
            if (!watched) {
                return new SessionWatchResponse(false, "Session Not Watched: " + sessionId);
            }

            Long endTimeMillis = null;
            if (session.getEndTime() != null) {
                endTimeMillis = session.getEndTime().atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
            }

            String currentWinnerUsername = resolveWinnerUsername(session.getCurrentWinnerId());

            Double availableBalance = userService.getAvailableBalance(request.getUserId());

            return new SessionWatchResponse(true, "Watching session", session.getId(),
                                        session.getCurrentPrice(), session.getCurrentWinnerId(), currentWinnerUsername,
                                        session.getStatus(), endTimeMillis, getMinimumNextBid(session),
                                        availableBalance);
        } catch (Exception e) {
            e.printStackTrace();
            return new SessionWatchResponse(false, "Watch session failed: unexpected server error");
        }
    }

    private String resolveWinnerUsername(String winnerId) {
        if (winnerId == null || winnerId.isBlank()) {
            return null;
        }

        try {
            User user = userService.getUserById(winnerId);
            return user.getUsername();
        } catch (UserNotFoundException e) {
            return null;
        }
    }

    public SessionWatchResponse unwatchSession(UnwatchSessionRequest request, AuctionSessionObserver observer) {
        try {
            if (observer == null) {
                return new SessionWatchResponse(false, "Observer is required");
            }
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new SessionWatchResponse(false, "Session ID is required");
            }
            String sessionId = request.getSessionId();

            boolean unwatched = sessionWatchRegistry.unwatchSession(sessionId, observer);
            if (!unwatched) {
                return new SessionWatchResponse(false, "Session was not watched or sessionId is invalid: " + sessionId);
            }

            return new SessionWatchResponse(true, "Unwatched session");
        } catch (Exception e) {
            e.printStackTrace();
            return new SessionWatchResponse(false, "Unwatch session failed: unexpected server error");
        }
    }

    public DashboardWatchResponse watchDashboard(WatchDashboardRequest request, DashboardObserver observer) {
        try {
            if (observer == null) {
                return new DashboardWatchResponse(false, "Dashboard observer is required");
            }

            boolean watched = dashboardWatchRegistry.watchDashboard(observer);
            if (!watched) {
                return new DashboardWatchResponse(false, "Dashboard Not Watched");
            }

            return new DashboardWatchResponse(true, "Watching dashboard");
        } catch (Exception e) {
            e.printStackTrace();
            return new DashboardWatchResponse(false, "Watch dashboard failed: unexpected server error");
        }
    }

    public DashboardWatchResponse unwatchDashboard(UnwatchDashboardRequest request, DashboardObserver observer) {
        try {
            if (observer == null) {
                return new DashboardWatchResponse(false, "Dashboard observer is required");
            }

            boolean unwatched = dashboardWatchRegistry.unwatchDashboard(observer);
            if (!unwatched) {
                return new DashboardWatchResponse(false, "Dashboard Not Watched");
            }

            return new DashboardWatchResponse(true, "Unwatched dashboard");
        } catch (Exception e) {
            e.printStackTrace();
            return new DashboardWatchResponse(false, "Unwatch dashboard failed: unexpected server error");
        }
    }

    private Double getMinimumNextBid(AuctionSession session) {
        if (session == null) {
            return null;
        }

        String status = session.getStatus();

        if (SessionService.STATUS_FINISHED.equals(status)
                || SessionService.STATUS_PAID.equals(status)
                || SessionService.STATUS_CANCELED.equals(status)) {
            return null;
        }

        return bidIncrementService.getMinimumNextBid(session.getCurrentPrice());
    }
}
