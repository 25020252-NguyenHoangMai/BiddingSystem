package com.auction.server.service;

import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;

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

    private final SessionDAO sessionDAO;
    private final UserDAO userDAO;

    public SessionService(SessionDAO sessionDAO, UserDAO userDAO) {
        if (sessionDAO == null) {
            throw new IllegalArgumentException("sessionDAO cannot be null");
        }
        if (userDAO == null) {
            throw new IllegalArgumentException("userDAO cannot be null");
        }

        this.sessionDAO = sessionDAO;
        this.userDAO = userDAO;
    }

    public AuctionSession createSession(Item item, LocalDateTime start, LocalDateTime end) {
        if (item == null) {
            throw new IllegalArgumentException("Item must not be null");
        }

        if (item.getId() == null || item.getId().isBlank()) {
            throw new IllegalArgumentException("Item id must not be empty");
        }

        if (start == null || end == null) {
            throw new IllegalArgumentException("Start time and end time must not be null");
        }

        if (!end.isAfter(start)) {
            throw new IllegalArgumentException("End time must be after start time");
        }

        if (sessionDAO.existsActiveSessionByItemId(item.getId())) {
            throw new IllegalArgumentException("This item already has an OPEN or RUNNING auction session");
        }

        //tạo sessionId ngẫu nhiên
        String sessionId = UUID.randomUUID().toString();
        //tạo AuctionSession
        AuctionSession session = new AuctionSession(sessionId, item, start, end);

        sessionDAO.insertSession(session, item);
        return session;
    }

    public AuctionSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        return sessionDAO.getSessionById(sessionId); // nhận sessionId, trả AuctionSession
    }

    public List<AuctionSession> getRunningSessions() {
        List<AuctionSession> runningSessions = new ArrayList<>();

        for (AuctionSession session : sessionDAO.getAllSessions()) {
            synchronized (session) {
                updateStatusByTime(session);

                if (STATUS_RUNNING.equals(session.getStatus())) {
                    runningSessions.add(session);
                }
            }
        }

        return runningSessions;
    }

    public void startSession(String sessionId) { //method bắt đầu phiên đấu giá
        AuctionSession session = requireSession(sessionId);

        //nếu là open -> hợp lệ -> set running
        synchronized (session) {
            if (!STATUS_OPEN.equals(session.getStatus())) {
                throw new IllegalStateException("Only OPEN session can be started");
            }

            LocalDateTime now = LocalDateTime.now();
            // có thể dùng updateStatusByTime nhưng nó có thể auto đổi status trước cả startSession rồi

            if (!now.isBefore(session.getEndTime())) {
                finalizeSession(session);
                throw new IllegalStateException("Cannot start an expired session");
            }

            if (now.isBefore(session.getStartTime())) {
                throw new IllegalStateException("Cannot start before start time");
            }

            updateSessionStatus(session, STATUS_RUNNING);
        }
    }

    public void finishSession(String sessionId) {
        AuctionSession session = requireSession(sessionId);

        synchronized (session) {
            if (STATUS_PAID.equals(session.getStatus()) || STATUS_CANCELED.equals(session.getStatus())) {
                throw new IllegalStateException("Session is already closed");
            }

            finalizeSession(session);
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

            LocalDateTime newEndTime = session.getEndTime().plus(extraTime);
            sessionDAO.updateEndTime(sessionId, newEndTime);
            session.setEndTime(newEndTime);
        }
    }

    public void refreshSessionStatus(String sessionId) {
        //method public vì updateStatusByTime là private
        AuctionSession session = requireSession(sessionId);

        synchronized (session) {
            updateStatusByTime(session);
        }
    }

    private AuctionSession requireSession(String sessionId) { // helper method
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
            updateSessionStatus(session, STATUS_RUNNING);
        }
        //status = open, >= startTime và < endTime => status = running

        if ((STATUS_OPEN.equals(session.getStatus()) || STATUS_RUNNING.equals(session.getStatus()))
                && !now.isBefore(session.getEndTime())) {
            finalizeSession(session);
        }
        //status = open/=running, >= endTime => status = finished
    }


    // không dùng method này cho đấu giá thật nữa, update bid đã ở trong phần atomic placebid() rồi
    public boolean updateCurrentBid(String sessionId, double newPrice, String bidderId) {
        if (sessionId == null || sessionId.isBlank()) {
            throw new IllegalArgumentException("Auction session not found: " + sessionId);
        }

        if (bidderId == null || bidderId.isBlank()) {
            throw new IllegalArgumentException("Bidder id must not be empty");
        }
        if (newPrice <= 0) {
            throw new IllegalArgumentException("Bid amount must be positive");
        }

        AuctionSession session = requireSession(sessionId);
        refreshSessionStatus(sessionId);
        session = requireSession(sessionId);

        LocalDateTime now = LocalDateTime.now();

        if (!STATUS_RUNNING.equals(session.getStatus())
                || now.isBefore(session.getStartTime())
                || !now.isBefore(session.getEndTime())) {
            return false;
        }

        if (newPrice <= session.getCurrentPrice()) {
            return false;
        }

        //return sessionDAO.updateCurrentBid(sessionId, newPrice, bidderId); quỳnh xóa cái này sửa thành bên duới
        try (java.sql.Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            return sessionDAO.updateCurrentBid(conn, sessionId, newPrice, bidderId);
        } catch (java.sql.SQLException e) {
            e.printStackTrace();
            return false;
        }

    }

    private void finalizeSession(AuctionSession session) {
        if (STATUS_PAID.equals(session.getStatus()) || STATUS_CANCELED.equals(session.getStatus())) {
            return;
        }

        String winnerId = session.getCurrentWinnerId();
        double finalPrice = session.getCurrentPrice();

        if (winnerId != null && !winnerId.isBlank() && finalPrice > 0) {
            String sellerId = session.getItem().getSellerId();
            if (sellerId == null || sellerId.isBlank()) {
                throw new IllegalStateException("Seller id is missing for auction item");
            }

            try (java.sql.Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    userDAO.deductReservedBalance(conn, winnerId, finalPrice);
                    userDAO.updateBalance(conn, sellerId, finalPrice);
                    sessionDAO.updateStatus(conn, session.getId(), STATUS_PAID);

                    conn.commit();
                    session.setStatus(STATUS_PAID);
                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }
            } catch (java.sql.SQLException e) {
                throw new RuntimeException("Finalize auction payment failed: " + e.getMessage(), e);
            }

            return;
        }

        updateSessionStatus(session, STATUS_FINISHED);

    }

    private void updateSessionStatus(AuctionSession session, String status) {
        try (java.sql.Connection conn =
                     com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            sessionDAO.updateStatus(conn, session.getId(), status);
            session.setStatus(status);
        } catch (java.sql.SQLException e) {
            throw new RuntimeException("Failed to update session status: " + e.getMessage(), e);
        }
    }

    public void finalizeExpiredSessions() {
        for (AuctionSession session : sessionDAO.getAllSessions()) {
            synchronized (session) {
                updateStatusByTime(session);
            }
        }
    }

}
