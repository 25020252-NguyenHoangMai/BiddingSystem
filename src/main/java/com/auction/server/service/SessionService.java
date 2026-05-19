package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;

import java.sql.Connection;
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

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                if (sessionDAO.existsActiveSessionByItemId(conn, item.getId())) {
                    throw new IllegalArgumentException("This item already has an OPEN or RUNNING auction session");
                }

                String sessionId = UUID.randomUUID().toString();
                AuctionSession session = new AuctionSession(sessionId, item, start, end);

                sessionDAO.insertSession(conn, session, item);

                conn.commit();
                return session;

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

            if (e instanceof IllegalArgumentException illegalArgumentException) {
                throw illegalArgumentException;
            }

            throw new AuctionException("Create auction session failed: " + e.getMessage());
        }
    }

    public AuctionSession getSession(String sessionId) {
        if (sessionId == null || sessionId.isBlank()) {
            return null;
        }

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            return sessionDAO.getSessionById(conn, sessionId);
        } catch (Exception e) {
            throw new AuctionException("Get auction session failed: " + e.getMessage());
        }
    }

    public List<AuctionSession> getRunningSessions() {
        List<AuctionSession> runningSessions = new ArrayList<>();

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            List<AuctionSession> sessions = sessionDAO.getAllSessions(conn);

            for (AuctionSession session : sessions) {
                synchronized (session) {
                    updateStatusByTime(session); // cũng tự mở connection, cần xem lại

                    if (STATUS_RUNNING.equals(session.getStatus())) {
                        runningSessions.add(session);
                    }
                }
            }

            return runningSessions;

        } catch (Exception e) {
            throw new AuctionException("Get running sessions failed: " + e.getMessage());
        }
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
        if (sessionId == null || sessionId.isBlank()) {
            throw new AuctionException("Session id is required.");
        }

        if (extraTime == null || extraTime.isZero() || extraTime.isNegative()) {
            throw new IllegalArgumentException("Extra time must be positive");
        }

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionId);

                if (session == null) {
                    throw new AuctionException("Auction session not found.");
                }

                if (!STATUS_RUNNING.equals(session.getStatus())) {
                    throw new IllegalStateException("Only RUNNING session can be extended");
                }

                LocalDateTime newEndTime = session.getEndTime().plus(extraTime);

                sessionDAO.updateEndTime(conn, sessionId, newEndTime);

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

            throw new AuctionException("Extend auction session failed: " + e.getMessage());
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

    public List<AuctionSession> finalizeExpiredSessions() {
        List<AuctionSession> finalizedSessions = new ArrayList<>();

        List<AuctionSession> sessions;

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            sessions = sessionDAO.getAllSessions(conn);
        } catch (Exception e) {
            throw new AuctionException("Load sessions for finalization failed: " + e.getMessage());
        }

        for (AuctionSession sessionSnapshot : sessions) {
            try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
                conn.setAutoCommit(false);

                try {
                    AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionSnapshot.getId());

                    if (session == null) {
                        conn.commit();
                        continue;
                    }

                    String oldStatus = session.getStatus();

                    LocalDateTime now = LocalDateTime.now();

                    boolean expired = (STATUS_OPEN.equals(oldStatus) || STATUS_RUNNING.equals(oldStatus))
                            && !now.isBefore(session.getEndTime());

                    if (!expired) {
                        conn.commit();
                        continue;
                    }

                    finalizeSession(conn, session);

                    String newStatus = session.getStatus();

                    conn.commit();

                    if (!oldStatus.equals(newStatus)
                            && (STATUS_PAID.equals(newStatus) || STATUS_FINISHED.equals(newStatus))) {
                        finalizedSessions.add(session);
                    }

                } catch (Exception e) {
                    conn.rollback();
                    throw e;
                } finally {
                    conn.setAutoCommit(true);
                }

            } catch (Exception e) {
                throw new AuctionException("Finalize expired session failed: " + e.getMessage());
            }
        }

        return finalizedSessions;
    }

    private void finalizeSession(Connection conn, AuctionSession session) {
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

            userDAO.deductReservedBalance(conn, winnerId, finalPrice);
            userDAO.updateBalance(conn, sellerId, finalPrice);
            sessionDAO.updateStatus(conn, session.getId(), STATUS_PAID);

            session.setStatus(STATUS_PAID);
            return;
        }

        sessionDAO.updateStatus(conn, session.getId(), STATUS_FINISHED);
        session.setStatus(STATUS_FINISHED);
    }

    public AuctionSession cancelSessionByAdmin(String adminId, String sessionId) {
        if (adminId == null || adminId.isBlank()) {
            throw new AuctionException("Admin id is required.");
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new AuctionException("Session id is required.");
        }

        User admin = userDAO.getUserById(adminId);
        if (admin == null || !"ADMIN".equalsIgnoreCase(admin.getRole())) {
            throw new AuctionException("Only admin can cancel auction.");
        }

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionId);
                if (session == null) {
                    throw new AuctionException("Auction session not found.");
                }

                String status = session.getStatus();

                if (STATUS_PAID.equals(status)) {
                    throw new AuctionException("Cannot cancel a paid auction.");
                }

                if (STATUS_CANCELED.equals(status)) {
                    throw new AuctionException("Auction is already canceled.");
                }

                String winnerId = session.getCurrentWinnerId();
                double currentPrice = session.getCurrentPrice();

                if (winnerId != null && !winnerId.isBlank() && currentPrice > 0) {
                    userDAO.updateReservedBalance(conn, winnerId, -currentPrice);
                }

                sessionDAO.updateStatus(conn, sessionId, STATUS_CANCELED);

                conn.commit();

                session.setStatus(STATUS_CANCELED);
                return session;

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
            throw new AuctionException("Cancel auction failed: " + e.getMessage());
        }
    }

    public AuctionSession cancelSessionBySeller(String sellerId, String sessionId) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new AuctionException("Admin id is required.");
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new AuctionException("Session id is required.");
        }

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionId);
                if (session == null) {
                    throw new AuctionException("Auction session not found.");
                }

                if (session.getItem() == null) {
                    throw new AuctionException("Auction item not found.");
                }

                String ownerSellerId = session.getItem().getSellerId();

                if (ownerSellerId == null || ownerSellerId.isBlank()) {
                    throw new AuctionException("Auction seller is missing.");
                }

                if (!sellerId.equals(ownerSellerId)) {
                    throw new AuctionException("You can only cancel your own auction.");
                }

                String status = session.getStatus();

                if (STATUS_PAID.equals(status)) {
                    throw new AuctionException("Cannot cancel a paid auction.");
                }
                if (STATUS_FINISHED.equals(status)) {
                    throw new AuctionException("Cannot cancel a finished auction");
                }
                if (STATUS_CANCELED.equals(status)) {
                    throw new AuctionException("Auction is already canceled.");
                }

                boolean hasBid = (session.getCurrentWinnerId() != null && !session.getCurrentWinnerId().isBlank());

                if (hasBid) {
                    throw new AuctionException("Cannot cancel auction after it has bids.");
                }

                sessionDAO.updateStatus(conn, sessionId, STATUS_CANCELED);

                conn.commit();

                session.setStatus(STATUS_CANCELED);
                return session;
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

            throw new AuctionException("Cancel auction failed: " + e.getMessage());
        }
    }

    public AuctionSession updateScheduleBySeller(
            String sellerId,
            String sessionId,
            LocalDateTime newStartTime,
            LocalDateTime newEndTime
    ) {
        if (sellerId == null || sellerId.isBlank()) {
            throw new AuctionException("Seller id is required.");
        }

        if (sessionId == null || sessionId.isBlank()) {
            throw new AuctionException("Session id is required.");
        }

        if (newEndTime == null) {
            throw new AuctionException("Auction end time is required.");
        }

        try (Connection conn = com.auction.server.dao.DatabaseManager.getInstance().getConnection()) {
            conn.setAutoCommit(false);

            try {
                AuctionSession session = sessionDAO.getSessionByIdForUpdate(conn, sessionId);

                if (session == null) {
                    throw new AuctionException("Auction session not found.");
                }

                validateSellerOwnsSession(sellerId, session);
                validateNoBidForSellerUpdate(session);

                String status = session.getStatus();

                if (STATUS_OPEN.equals(status)) {
                    updateOpenSessionSchedule(conn, session, newStartTime, newEndTime);
                } else if (STATUS_RUNNING.equals(status)) {
                    updateRunningSessionEndTime(conn, session, newStartTime, newEndTime);
                } else if (STATUS_CANCELED.equals(status)) {
                    throw new AuctionException("Cannot update a canceled auction.");
                } else if (STATUS_FINISHED.equals(status)) {
                    throw new AuctionException("Cannot update a finished auction.");
                } else if (STATUS_PAID.equals(status)) {
                    throw new AuctionException("Cannot update a paid auction.");
                } else {
                    throw new AuctionException("Cannot update auction with status: " + status);
                }

                conn.commit();
                return session;

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

            throw new AuctionException("Update auction schedule failed: " + e.getMessage());
        }
    }

    private void validateSellerOwnsSession(String sellerId, AuctionSession session) {
        if (session.getItem() == null) {
            throw new AuctionException("Auction item not found.");
        }

        String ownerSellerId = session.getItem().getSellerId();

        if (ownerSellerId == null || ownerSellerId.isBlank()) {
            throw new AuctionException("Auction seller is missing.");
        }

        if (!sellerId.equals(ownerSellerId)) {
            throw new AuctionException("You can only update your own auction.");
        }
    }

    private void validateNoBidForSellerUpdate(AuctionSession session) {
        String currentWinnerId = session.getCurrentWinnerId();

        boolean hasBid = currentWinnerId != null && !currentWinnerId.isBlank();

        if (hasBid) {
            throw new AuctionException("Cannot update auction after it has bids.");
        }
    }

    private void updateOpenSessionSchedule(
            Connection conn,
            AuctionSession session,
            LocalDateTime newStartTime,
            LocalDateTime newEndTime
    ) {
        if (newStartTime == null) {
            throw new AuctionException("Auction start time is required.");
        }

        if (!newEndTime.isAfter(newStartTime)) {
            throw new AuctionException("Auction end time must be after start time.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (newStartTime.isBefore(now.minusMinutes(1))) {
            throw new AuctionException("Auction start time cannot be in the past.");
        }

        sessionDAO.updateSchedule(conn, session.getId(), newStartTime, newEndTime);

        session.setStartTime(newStartTime);
        session.setEndTime(newEndTime);
    }

    private void updateRunningSessionEndTime(
            Connection conn,
            AuctionSession session,
            LocalDateTime newStartTime,
            LocalDateTime newEndTime
    ) {
        if (newStartTime != null && !newStartTime.equals(session.getStartTime())) {
            throw new AuctionException("Cannot update start time after auction has started.");
        }

        LocalDateTime now = LocalDateTime.now();

        if (!newEndTime.isAfter(now)) {
            throw new AuctionException("Auction end time must be in the future.");
        }

        if (!newEndTime.isAfter(session.getStartTime())) {
            throw new AuctionException("Auction end time must be after start time.");
        }

        sessionDAO.updateEndTime(conn, session.getId(), newEndTime);

        session.setEndTime(newEndTime);
    }

}
