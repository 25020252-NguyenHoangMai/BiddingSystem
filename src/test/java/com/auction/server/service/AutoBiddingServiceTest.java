package com.auction.server.service;

import com.auction.model.AuctionSession;
import com.auction.model.AutoBid;
import com.auction.model.User;
import com.auction.server.dao.AutoBidDAO;
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
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class AutoBiddingServiceTest {

    @Mock private BidValidationService bidValidationService;
    @Mock private SessionService sessionService;
    @Mock private BidIncrementService bidIncrementService;
    @Mock private BidTransactionExecutor bidTransactionExecutor;
    @Mock private UserService userService;
    @Mock private AutoBidDAO autoBidDAO;
    @Mock private AntiSnipingService antiSnipingService;

    @Mock private ProxyAutoBidResolver proxyAutoBidResolver;

    @Mock private Connection mockConn;
    @Mock private DatabaseManager mockDbManager;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private AutoBiddingService autoBiddingService;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        autoBiddingService = new AutoBiddingService(bidValidationService, sessionService, bidIncrementService,
                bidTransactionExecutor, userService, autoBidDAO, antiSnipingService, proxyAutoBidResolver);

        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);
    }

    @AfterEach
    void cleanUp() {
        if (mockedStaticDbManager != null) {
            mockedStaticDbManager.close();
        }
    }

    @Nested
    class ConstructorTests {
        @Test
        void nullDependencies_ThrowsException() {

            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(null, sessionService, bidIncrementService, bidTransactionExecutor, userService, autoBidDAO, antiSnipingService, proxyAutoBidResolver));
            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(bidValidationService, null, bidIncrementService, bidTransactionExecutor, userService, autoBidDAO, antiSnipingService, proxyAutoBidResolver));
            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(bidValidationService, sessionService, null, bidTransactionExecutor, userService, autoBidDAO, antiSnipingService, proxyAutoBidResolver));
            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(bidValidationService, sessionService, bidIncrementService, null, userService, autoBidDAO, antiSnipingService, proxyAutoBidResolver));
            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(bidValidationService, sessionService, bidIncrementService, bidTransactionExecutor, null, autoBidDAO, antiSnipingService, proxyAutoBidResolver));
            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(bidValidationService, sessionService, bidIncrementService, bidTransactionExecutor, userService, null, antiSnipingService, proxyAutoBidResolver));
            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(bidValidationService, sessionService, bidIncrementService, bidTransactionExecutor, userService, autoBidDAO, null, proxyAutoBidResolver));


            assertThrows(IllegalArgumentException.class, () -> new AutoBiddingService(bidValidationService, sessionService, bidIncrementService, bidTransactionExecutor, userService, autoBidDAO, antiSnipingService, null));
        }
    }

    @Nested
    class SetAutoBidTests {
        @Test
        void validateInput_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> autoBiddingService.setAutoBid(null, "B1", 100));
            assertThrows(IllegalArgumentException.class, () -> autoBiddingService.setAutoBid("S1", null, 100));
            assertThrows(IllegalArgumentException.class, () -> autoBiddingService.setAutoBid("S1", "B1", 0));
        }

        @Test
        void sessionNotFound_ReturnsFalse() {
            when(sessionService.getSession("S1")).thenReturn(null);
            BidResult result = autoBiddingService.setAutoBid("S1", "B1", 100);
            assertFalse(result.isSuccess());
            assertEquals("Auction session not found: S1", result.getMessage());
        }

        @Test
        void sessionNotBiddable_ReturnsFalse() {
            AuctionSession session = new AuctionSession();
            session.setId("S1");
            when(sessionService.getSession("S1")).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(false);
            when(bidValidationService.buildNotBiddableMessage(session)).thenReturn("Not biddable");

            BidResult result = autoBiddingService.setAutoBid("S1", "B1", 100);
            assertFalse(result.isSuccess());
            assertEquals("Not biddable", result.getMessage());
        }

        @Test
        void maxAmountLessThanMinimumNextBid_ReturnsFalse() {
            AuctionSession session = new AuctionSession();
            session.setId("S1");
            session.setCurrentPrice(50.0);
            when(sessionService.getSession("S1")).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(bidIncrementService.getMinimumNextBid(50.0)).thenReturn(60.0);

            BidResult result = autoBiddingService.setAutoBid("S1", "B1", 55.0);
            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Auto bid max amount must be at least 60.0"));
        }

        @Test
        void success_AlreadyWinner_ReturnsTrue() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("S1");
            session.setCurrentPrice(50.0);
            session.setCurrentWinnerId("B1");

            when(sessionService.getSession("S1")).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(bidIncrementService.getMinimumNextBid(50.0)).thenReturn(60.0);

            User mockUser = mock(User.class);
            when(mockUser.getUsername()).thenReturn("nguyen_cong_minh");
            when(userService.getUserById("B1")).thenReturn(mockUser);


            BidExecutionResult mockResolveResult = new BidExecutionResult(true, "Auto bid enabled", "S1", 100.0, "B1", "RUNNING");
            when(proxyAutoBidResolver.resolve("S1")).thenReturn(mockResolveResult);

            BidResult result = autoBiddingService.setAutoBid("S1", "B1", 100.0);

            assertTrue(result.isSuccess());
            assertEquals("Auto bid enabled", result.getMessage());
            verify(autoBidDAO).upsertAutoBid(any(Connection.class), anyString(), eq("S1"), eq("B1"), eq(100.0));
        }
    }

    @Nested
    class ProcessAutoBidsAfterBidTests {
        @Test
        void noSession_ReturnsEmptyList() {
            assertTrue(autoBiddingService.processAutoBidsAfterBid(null, "B1").isEmpty());
            assertTrue(autoBiddingService.processAutoBidsAfterBid("  ", "B1").isEmpty());
        }

        @Test
        void noSessionFound_ReturnsEmptyList() {
            when(sessionService.getSession("S1")).thenReturn(null);
            assertTrue(autoBiddingService.processAutoBidsAfterBid("S1", "B1").isEmpty());
        }

        @Test
        void sessionNotBiddable_ReturnsEmptyList() {
            AuctionSession session = new AuctionSession();
            session.setId("S1");
            when(sessionService.getSession("S1")).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(false);
            assertTrue(autoBiddingService.processAutoBidsAfterBid("S1", "B1").isEmpty());
        }

        @Test
        void noActiveAutoBids_ReturnsEmptyList() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("S1");
            when(sessionService.getSession("S1")).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(autoBidDAO.getActiveAutoBidsBySession(any(Connection.class), eq("S1"))).thenReturn(Collections.emptyList());

            assertTrue(autoBiddingService.processAutoBidsAfterBid("S1", "B1").isEmpty());
        }

        @Test
        void activeAutoBid_SuccessfulBid() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("S1");
            session.setCurrentPrice(50.0);
            session.setCurrentWinnerId("B1");

            AuctionSession updatedSession = new AuctionSession();
            updatedSession.setId("S1");
            updatedSession.setCurrentPrice(60.0);
            updatedSession.setCurrentWinnerId("B2");
            when(sessionService.getSession("S1")).thenReturn(session, session, updatedSession);

            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);

            AutoBid autoBid = new AutoBid();
            autoBid.setId("A1");
            autoBid.setSessionId("S1");
            autoBid.setBidderId("B2");
            autoBid.setMaxBidAmount(100.0);
            autoBid.setActive(true);

            when(autoBidDAO.getActiveAutoBidsBySession(any(Connection.class), eq("S1"))).thenReturn(Arrays.asList(autoBid));
            when(bidIncrementService.getMinimumNextBid(50.0)).thenReturn(60.0);
            when(autoBidDAO.getActiveAutoBid(any(Connection.class), eq("S1"), eq("B2"))).thenReturn(autoBid);

            BidExecutionResult executionResult = new BidExecutionResult(true, "Success", "S1", 60.0, "B2", "RUNNING");
            when(bidTransactionExecutor.execute("S1", "B2", 60.0)).thenReturn(executionResult);

            User mockWinnerB1 = mock(User.class);
            when(mockWinnerB1.getUsername()).thenReturn("nguyen_cong_minh");
            when(userService.getUserById("B1")).thenReturn(mockWinnerB1);

            User mockWinnerB2 = mock(User.class);
            when(mockWinnerB2.getUsername()).thenReturn("hung_auto_bidder");
            when(userService.getUserById("B2")).thenReturn(mockWinnerB2);

            List<BidResult> results = autoBiddingService.processAutoBidsAfterBid("S1", "B1");

            assertEquals(1, results.size());
            assertTrue(results.get(0).isSuccess());
            assertEquals("B2", results.get(0).getCurrentWinnerId());
        }
    }
}