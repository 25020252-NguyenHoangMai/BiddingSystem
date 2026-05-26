package com.auction.server.service;

import com.auction.dto.BidHistoryEntryDTO;
import com.auction.dto.SessionHistoryItemDTO;
import com.auction.exception.AuctionException;
import com.auction.model.BidTransaction;
import com.auction.model.User;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class BidHistoryServiceTest {

    @Mock private BidDAO bidDAO;
    @Mock private UserService userService;
    @Mock private Connection mockConn;
    @Mock private DatabaseManager mockDbManager;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private BidHistoryService bidHistoryService;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        bidHistoryService = new BidHistoryService(bidDAO, userService);

        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);
    }

    @AfterEach
    void tearDown() {
        if (mockedStaticDbManager != null) {
            mockedStaticDbManager.close();
        }
    }

    @Nested
    class ConstructorTests {
        @Test
        void nullDependencies_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> new BidHistoryService(null, userService));
            assertThrows(IllegalArgumentException.class, () -> new BidHistoryService(bidDAO, null));
            assertDoesNotThrow(() -> new BidHistoryService(bidDAO, userService));
        }
    }

    @Nested
    class GetBidHistoryTests {
        @Test
        void nullOrBlankSessionId_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> bidHistoryService.getBidHistory(null));
            assertThrows(IllegalArgumentException.class, () -> bidHistoryService.getBidHistory("   "));
        }

        @Test
        void sqlException_ThrowsAuctionException() throws SQLException {

            when(mockDbManager.getConnection()).thenThrow(new SQLException("DB Error"));

            assertThrows(AuctionException.class, () -> bidHistoryService.getBidHistory("SS-1"));
        }

        @Test
        void validSession_ReturnsHistoryList() throws SQLException {

            BidTransaction bid1 = mock(BidTransaction.class);
            when(bid1.getBidderId()).thenReturn("U1");
            when(bid1.getBidAmount()).thenReturn(100.0);
            when(bid1.getBidTime()).thenReturn(LocalDateTime.now());

            BidTransaction bid2 = mock(BidTransaction.class);
            when(bid2.getBidderId()).thenReturn("U2");
            when(bid2.getBidAmount()).thenReturn(150.0);
            when(bid2.getBidTime()).thenReturn(null);

            BidTransaction bid3 = mock(BidTransaction.class);
            when(bid3.getBidderId()).thenReturn(null);
            when(bid3.getBidAmount()).thenReturn(200.0);
            when(bid3.getBidTime()).thenReturn(LocalDateTime.now());

            User user1 = mock(User.class);
            when(user1.getUsername()).thenReturn("nguyen_cong_minh");

            when(userService.getUserById("U1")).thenReturn(user1);
            when(userService.getUserById("U2")).thenThrow(new RuntimeException("User not found"));
            when(bidDAO.getBidsBySession(any(Connection.class), eq("SS-1"))).thenReturn(List.of(bid1, bid2, bid3));

            List<BidHistoryEntryDTO> history = bidHistoryService.getBidHistory("SS-1");

            assertEquals(3, history.size());

            assertEquals("nguyen_cong_minh", history.get(0).getBidderUsername());
            assertEquals(100.0, history.get(0).getBidAmount());
            assertNotEquals(0L, history.get(0).getBidTimeMillis());

            assertEquals("Unknown", history.get(1).getBidderUsername());
            assertEquals(150.0, history.get(1).getBidAmount());
            assertEquals(0L, history.get(1).getBidTimeMillis());

            assertNull(history.get(2).getBidderUsername());
            assertEquals(200.0, history.get(2).getBidAmount());
        }

    }

    @Nested
    class GetSessionHistoryTests {
        @Test
        void nullOrBlankUserId_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> bidHistoryService.getSessionHistory(null));
            assertThrows(IllegalArgumentException.class, () -> bidHistoryService.getSessionHistory("  "));
        }

        @Test
        void sqlException_ThrowsAuctionException() throws SQLException {

            when(mockDbManager.getConnection()).thenThrow(new SQLException("DB Error"));

            assertThrows(AuctionException.class, () -> bidHistoryService.getSessionHistory("U1"));
        }

        @Test
        void validUserId_ReturnsHistoryList() throws SQLException {
            SessionHistoryItemDTO dto = new SessionHistoryItemDTO();
            when(bidDAO.getSessionHistoryByBidder(any(Connection.class), eq("U1")))
                    .thenReturn(List.of(dto));

            List<SessionHistoryItemDTO> result = bidHistoryService.getSessionHistory("U1");

            assertEquals(1, result.size());
            assertEquals(dto, result.get(0));
        }
    }
}