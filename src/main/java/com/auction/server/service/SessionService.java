package com.auction.server.service;

import com.auction.model.AuctionSession;
import com.auction.model.Item;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

public class SessionService { // Quản lí phiên đấu giá
    public static final String STATUS_OPEN = "OPEN";
    public static final String STATUS_RUNNING = "RUNNING";
    public static final String STATUS_FINISHED = "FINISHED";
    public static final String STATUS_PAID = "PAID";
    public static final String STATUS_CANCELED = "CANCELED";

    private final ConcurrentMap<String, AuctionSession> sessions = new ConcurrentHashMap<>();

    public AuctionSession createSession(Item item, LocalDateTime start, LocalDateTime end) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }

        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        String sessionId = UUID.randomUUID().toString();
        AuctionSession session = new AuctionSession(sessionId, item, start, end);

        sessions.put(sessionId, session);
        return session;
    }

    public AuctionSession getSession(String sessionId) {
        if (sessionId == null) {
            return null;
        }

        return sessions.get(sessionId);
    }

    public List<AuctionSession> getRunningSessions() {
        List<AuctionSession> runningSessions = new ArrayList<>();

        for (AuctionSession session : sessions.values()) {
            synchronized (session) {
                updateStatusByTime(session);

                if (STATUS_RUNNING.equals(session.getStatus())) {
                    runningSessions.add(session);
                }
            }
        }

        return runningSessions;
    }

    public void startSession(String sessionId) {
        AuctionSession session = requireSession(sessionId);

        synchronized (session) {
            if (!STATUS_OPEN.equals(session.getStatus())) {
                throw new IllegalStateException("Only OPEN session can be started");
            }

            LocalDateTime now = LocalDateTime.now();

            if (!now.isBefore(session.getEndTime())) {
                session.setStatus(STATUS_FINISHED);
                throw new IllegalStateException("Cannot start an expired session");
            }

            session.setStatus(STATUS_RUNNING);
        }
    }

    public void finishSession(String sessionId) {
        AuctionSession session = requireSession(sessionId);

        synchronized (session) {
            if (STATUS_PAID.equals(session.getStatus()) || STATUS_CANCELED.equals(session.getStatus())) {
                throw new IllegalStateException("Session is already closed");
            }

            session.setStatus(STATUS_FINISHED);
        }
    }

    public boolean isBiddable(String sessionId) {
        AuctionSession session = getSession(sessionId);

        if (session == null) {
            return false;
        }

        synchronized (session) {
            updateStatusByTime(session);

            LocalDateTime now = LocalDateTime.now();

            return STATUS_RUNNING.equals(session.getStatus())
                    && !now.isBefore(session.getStartTime())
                    && now.isBefore(session.getEndTime());
        }
    }

    public void extendSession(String sessionId, Duration extraTime) {
        AuctionSession session = requireSession(sessionId);

        if (extraTime == null || extraTime.isZero() || extraTime.isNegative()) {
            throw new IllegalArgumentException("Extra time must be positive");
        }

        synchronized (session) {
            if (!STATUS_RUNNING.equals(session.getStatus())) {
                throw new IllegalStateException("Only RUNNING session can be extended");
            }

            session.setEndTime(session.getEndTime().plus(extraTime));
        }
    }

    public void refreshSessionStatus(String sessionId) {
        AuctionSession session = requireSession(sessionId);

        synchronized (session) {
            updateStatusByTime(session);
        }
    }

    private AuctionSession requireSession(String sessionId) {
        AuctionSession session = getSession(sessionId);

        if (session == null) {
            throw new IllegalArgumentException("Auction session not found: " + sessionId);
        }

        return session;
    }

    private void updateStatusByTime(AuctionSession session) {
        LocalDateTime now = LocalDateTime.now();

        if (STATUS_OPEN.equals(session.getStatus())
                && !now.isBefore(session.getStartTime())
                && now.isBefore(session.getEndTime())) {
            session.setStatus(STATUS_RUNNING);
        }

        if ((STATUS_OPEN.equals(session.getStatus()) || STATUS_RUNNING.equals(session.getStatus()))
                && !now.isBefore(session.getEndTime())) {
            session.setStatus(STATUS_FINISHED);
        }
    }

}
