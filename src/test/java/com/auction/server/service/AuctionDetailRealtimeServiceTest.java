package com.auction.server.service;

import com.auction.dto.ItemDTO;
import com.auction.model.AuctionSession;
import com.auction.response.BidUpdateResponse;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.SessionDAO;
import com.auction.server.realtime.SessionWatchRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

class AuctionDetailRealtimeServiceTest {

    @Mock private SessionWatchRegistry sessionWatchRegistry;
    @Mock private SessionDAO sessionDAO;
    @Mock private BidDAO bidDAO;
    @Mock private ItemService itemService;
    @Mock private Connection mockConn;
    @Mock private DatabaseManager mockDbManager;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private AuctionDetailRealtimeService realtimeService;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        realtimeService = new AuctionDetailRealtimeService(sessionWatchRegistry, sessionDAO, bidDAO, itemService);

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
    class BroadcastItemUpdatedTests {

        @Test
        void nullOrBlankItem_DoesNothing() {

            realtimeService.broadcastItemUpdated(null, "Test");

            ItemDTO blankSessionItem = new ItemDTO();
            realtimeService.broadcastItemUpdated(blankSessionItem, "Test");

            blankSessionItem.setSessionId("   ");
            realtimeService.broadcastItemUpdated(blankSessionItem, "Test");

            verify(sessionWatchRegistry, never()).broadcastBidUpdate(anyString(), any());
        }

        @Test
        void validItem_BroadcastsCorrectly() {

            ItemDTO item = new ItemDTO();
            item.setSessionId("SS-123");
            item.setCurrentPrice(500.0);
            item.setCurrentWinnerUsername("nguyen_cong_minh");
            item.setSessionStatus("RUNNING");
            item.setEndTimeMillis(1700000000000L);
            item.setMinimumNextBid(550.0);

            realtimeService.broadcastItemUpdated(item, "Item updated");

            ArgumentCaptor<BidUpdateResponse> captor = ArgumentCaptor.forClass(BidUpdateResponse.class);
            verify(sessionWatchRegistry).broadcastBidUpdate(eq("SS-123"), captor.capture());

            BidUpdateResponse response = captor.getValue();
            assertTrue(response.isSuccess());
            assertEquals("Item updated", response.getMessage());
            assertEquals("SS-123", response.getSessionId());
            assertEquals(500.0, response.getCurrentPrice());
            assertEquals("nguyen_cong_minh", response.getCurrentWinnerUsername());
            assertEquals("RUNNING", response.getStatus());
            assertEquals(1700000000000L, response.getEndTimeMillis());
            assertEquals(550.0, response.getMinimumNextBid());
        }
    }

    @Nested
    class BroadcastAuctionCanceledTests {

        @Test
        void nullOrBlankSession_DoesNothing() {

            realtimeService.broadcastAuctionCanceled(null, "Test");
            realtimeService.broadcastAuctionCanceled(new AuctionSession(), "Test");

            AuctionSession blankSession = new AuctionSession();
            blankSession.setId("  ");
            realtimeService.broadcastAuctionCanceled(blankSession, "Test");

            verify(sessionWatchRegistry, never()).broadcastBidUpdate(anyString(), any());
        }

        @Test
        void validSession_WithEndTime_BroadcastsCorrectly() {

            AuctionSession session = new AuctionSession();
            session.setId("SS-456");
            session.setCurrentPrice(100.0);
            session.setCurrentWinnerId("BIDDER_1");
            session.setStatus("CANCELED");
            session.setEndTime(LocalDateTime.now().plusDays(1));

            realtimeService.broadcastAuctionCanceled(session, "Auction canceled");

            ArgumentCaptor<BidUpdateResponse> captor = ArgumentCaptor.forClass(BidUpdateResponse.class);
            verify(sessionWatchRegistry).broadcastBidUpdate(eq("SS-456"), captor.capture());

            BidUpdateResponse response = captor.getValue();
            assertTrue(response.isSuccess());
            assertEquals("SS-456", response.getSessionId());
            assertEquals("CANCELED", response.getStatus());
            assertEquals("BIDDER_1", response.getCurrentWinnerId());
            assertNotNull(response.getEndTimeMillis());
        }

        @Test
        void validSession_WithoutEndTime_BroadcastsCorrectly() {

            AuctionSession session = new AuctionSession();
            session.setId("SS-789");
            session.setStatus("CANCELED");
            session.setEndTime(null);

            realtimeService.broadcastAuctionCanceled(session, "Auction canceled");

            ArgumentCaptor<BidUpdateResponse> captor = ArgumentCaptor.forClass(BidUpdateResponse.class);
            verify(sessionWatchRegistry).broadcastBidUpdate(eq("SS-789"), captor.capture());

            assertNull(captor.getValue().getEndTimeMillis());
        }
    }

    @Nested
    class BroadcastUsernameChangedTests {

        @Test
        void nullOrBlankUserId_DoesNothing() throws SQLException {
            realtimeService.broadcastUsernameChanged(null);
            realtimeService.broadcastUsernameChanged("");
            verify(mockDbManager, never()).getConnection();
        }

        @Test
        void dbException_CatchesAndReturnsSilently() throws SQLException {

            when(mockDbManager.getConnection()).thenThrow(new SQLException("Database down"));

            assertDoesNotThrow(() -> realtimeService.broadcastUsernameChanged("U1"));
            verify(itemService, never()).getAuctionDetailDTO(anyString());
        }

        @Test
        void validUserId_CombinesIdsAndBroadcasts() throws SQLException {

            when(sessionDAO.getVisibleSessionIdsBySellerId(any(Connection.class), eq("U1")))
                    .thenReturn(List.of("SS-1", "SS-2"));


            when(bidDAO.getVisibleSessionIdsByBidderId(any(Connection.class), eq("U1")))
                    .thenReturn(List.of("SS-2", "SS-3"));

            ItemDTO mockDto1 = new ItemDTO(); mockDto1.setSessionId("SS-1");
            ItemDTO mockDto2 = new ItemDTO(); mockDto2.setSessionId("SS-2");
            ItemDTO mockDto3 = new ItemDTO(); mockDto3.setSessionId("SS-3");

            when(itemService.getAuctionDetailDTO("SS-1")).thenReturn(mockDto1);
            when(itemService.getAuctionDetailDTO("SS-2")).thenReturn(mockDto2);
            when(itemService.getAuctionDetailDTO("SS-3")).thenReturn(mockDto3);

            realtimeService.broadcastUsernameChanged("U1");


            verify(itemService, times(3)).getAuctionDetailDTO(anyString());


            verify(sessionWatchRegistry, times(3)).broadcastBidUpdate(anyString(), any(BidUpdateResponse.class));
        }

        @Test
        void oneSessionFails_ContinuesToNext() throws SQLException {

            when(sessionDAO.getVisibleSessionIdsBySellerId(any(Connection.class), eq("U1")))
                    .thenReturn(List.of("SS-1", "SS-2"));
            when(bidDAO.getVisibleSessionIdsByBidderId(any(Connection.class), eq("U1")))
                    .thenReturn(List.of());


            when(itemService.getAuctionDetailDTO("SS-1")).thenThrow(new RuntimeException("Item deleted"));

            ItemDTO mockDto2 = new ItemDTO(); mockDto2.setSessionId("SS-2");
            when(itemService.getAuctionDetailDTO("SS-2")).thenReturn(mockDto2);

            assertDoesNotThrow(() -> realtimeService.broadcastUsernameChanged("U1"));


            verify(sessionWatchRegistry, times(1)).broadcastBidUpdate(eq("SS-2"), any());
        }
    }
}