package com.auction.server.controller;

import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.User;
import com.auction.request.UnwatchSessionRequest;
import com.auction.request.WatchSessionRequest;
import com.auction.response.BidUpdateResponse;
import com.auction.response.Response;
import com.auction.response.SessionWatchResponse;
import com.auction.server.realtime.AuctionSessionObserver;
import com.auction.server.realtime.SessionWatchRegistry;
import com.auction.server.service.SessionService;
import com.auction.server.service.UserService;

import java.time.ZoneId;

public class RealTimeController {
    private final SessionWatchRegistry sessionWatchRegistry;
    private final SessionService sessionService;
    private final UserService userService;

    public RealTimeController(SessionWatchRegistry sessionWatchRegistry, SessionService sessionService,
                              UserService userService) {
        this.sessionWatchRegistry = sessionWatchRegistry;
        this.sessionService = sessionService;
        this.userService = userService;
    }

    public Response watchSession(WatchSessionRequest request, AuctionSessionObserver observer) {
        try {
            if (observer == null) {
                return new SessionWatchResponse(false, "Observer is required");
            }
            if (request.getSessionId() == null || request.getSessionId().isBlank()) {
                return new SessionWatchResponse(false, "Session ID is required");
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

            return new BidUpdateResponse(true, "Watching session", session.getId(),
                                        session.getCurrentPrice(), session.getCurrentWinnerId(), currentWinnerUsername,
                                        session.getStatus(), endTimeMillis);
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
}
