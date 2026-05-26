package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.model.AuctionSession;
import com.auction.model.AutoBid;
import com.auction.model.BidTransaction;
import com.auction.server.dao.*;
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
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class ProxyAutoBidResolverTest {

    @Mock private AutoBidDAO autoBidDAO;
    @Mock private SessionDAO sessionDAO;
    @Mock private UserDAO userDAO;
    @Mock private BidDAO bidDAO;
    @Mock private BidIncrementService bidIncrementService;
    @Mock private BidValidationService bidValidationService;
    @Mock private BidReservationCalculator bidReservationCalculator;
    @Mock private Connection mockConn;
    @Mock private DatabaseManager mockDbManager;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private ProxyAutoBidResolver resolver;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        resolver = new ProxyAutoBidResolver(autoBidDAO, sessionDAO, userDAO, bidDAO, bidIncrementService,
                bidValidationService, bidReservationCalculator);

        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);
    }

    @AfterEach
    void Cleanup() {
        if (mockedStaticDbManager != null) {
            mockedStaticDbManager.close();
        }
    }

    @Nested
    class ConnectionExceptionTests {
        @Test
        void getConnFails_ThrowsException() throws SQLException {
            when(mockDbManager.getConnection()).thenThrow(new SQLException("DB Down"));
            assertThrows(AuctionException.class, () -> resolver.resolve("SS-1"));
        }

        @Test
        void rollbackFails_HandledGracefully() throws SQLException {

            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1")))
                    .thenThrow(new RuntimeException("Lock timeout"));


            doThrow(new SQLException("Rollback fail")).when(mockConn).rollback();

            assertThrows(AuctionException.class, () -> resolver.resolve("SS-1"));
        }
    }

    @Nested
    class ResolveInsideTransactionTests {

        @Test
        void sessionNotFound_ReturnsFalse() throws SQLException {
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(null);

            BidExecutionResult result = resolver.resolve("SS-1");

            assertFalse(result.isSuccess());
            assertEquals("Auction session not found", result.getMessage());
        }

        @Test
        void sessionNotBiddable_ReturnsFalse() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(false);
            when(bidValidationService.buildNotBiddableMessage(session)).thenReturn("Not biddable");

            BidExecutionResult result = resolver.resolve("SS-1");

            assertFalse(result.isSuccess());
            assertEquals("Not biddable", result.getMessage());
        }

        @Test
        void noActiveAutoBids_ReturnsFalse() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(autoBidDAO.getActiveAutoBidsBySessionSorted(any(Connection.class), eq("SS-1")))
                    .thenReturn(Collections.emptyList());

            BidExecutionResult result = resolver.resolve("SS-1");

            assertFalse(result.isSuccess());
            assertEquals("No active auto bids", result.getMessage());
        }

        @Test
        void autoBidDidNotImprovePrice_ReturnsFalse() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            session.setCurrentWinnerId("U1");
            session.setCurrentPrice(100.0);

            AutoBid highest = new AutoBid();
            highest.setBidderId("U2");
            highest.setMaxBidAmount(90.0);

            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(autoBidDAO.getActiveAutoBidsBySessionSorted(any(Connection.class), eq("SS-1")))
                    .thenReturn(List.of(highest));
            when(bidIncrementService.getMinimumNextBid(anyDouble())).thenReturn(110.0);

            BidExecutionResult result = resolver.resolve("SS-1");

            assertFalse(result.isSuccess());
            assertEquals("Auto bid did not improve current price", result.getMessage());
            verify(mockConn).rollback();
        }

        @Test
        void sameWinner_SamePrice_ReturnsFalse() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            session.setCurrentWinnerId("U1");
            session.setCurrentPrice(100.0);

            AutoBid highest = new AutoBid();
            highest.setBidderId("U1"); // Vẫn là U1
            highest.setMaxBidAmount(200.0);

            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(autoBidDAO.getActiveAutoBidsBySessionSorted(any(Connection.class), eq("SS-1")))
                    .thenReturn(List.of(highest));
            when(bidIncrementService.getMinimumNextBid(anyDouble())).thenReturn(100.0);

            BidExecutionResult result = resolver.resolve("SS-1");

            assertFalse(result.isSuccess());
            assertEquals("Auto bid did not change current state", result.getMessage());
        }

        @Test
        void insufficientBalance_ThrowsException() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            session.setCurrentPrice(100.0);

            AutoBid highest = new AutoBid();
            highest.setBidderId("U1");
            highest.setMaxBidAmount(200.0);

            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(autoBidDAO.getActiveAutoBidsBySessionSorted(any(Connection.class), eq("SS-1")))
                    .thenReturn(List.of(highest));
            when(bidIncrementService.getMinimumNextBid(anyDouble())).thenReturn(110.0);
            when(bidReservationCalculator.calculateReserveChange(any(), any(), anyDouble(), anyDouble()))
                    .thenReturn(110.0);

            UserBalance lowBalance = new UserBalance(50.0, 0.0);
            when(userDAO.getBalanceForUpdate(any(Connection.class), eq("U1"))).thenReturn(lowBalance);

            assertThrows(InsufficientBalanceException.class, () -> resolver.resolve("SS-1"));
            verify(mockConn).rollback();
        }

        @Test
        void successfulProxyBid_UpdatesSessionAndBids() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            session.setCurrentWinnerId("U1");
            session.setCurrentPrice(100.0);

            AutoBid highest = new AutoBid();
            highest.setBidderId("U2");
            highest.setMaxBidAmount(300.0);

            AutoBid second = new AutoBid();
            second.setBidderId("U3");
            second.setMaxBidAmount(200.0);

            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(autoBidDAO.getActiveAutoBidsBySessionSorted(any(Connection.class), eq("SS-1")))
                    .thenReturn(List.of(highest, second));
            when(bidIncrementService.getMinimumNextBid(200.0)).thenReturn(210.0);
            when(bidReservationCalculator.calculateReserveChange(eq("U1"), eq("U2"), eq(100.0), eq(210.0)))
                    .thenReturn(210.0);

            UserBalance richBalance = new UserBalance(1000.0, 0.0);
            when(userDAO.getBalanceForUpdate(any(Connection.class), eq("U2"))).thenReturn(richBalance);
            when(sessionDAO.updateCurrentBid(any(Connection.class), eq("SS-1"), eq(210.0), eq("U2"))).thenReturn(true);

            BidExecutionResult result = resolver.resolve("SS-1");

            assertTrue(result.isSuccess());
            assertEquals(210.0, result.getCurrentPrice());
            assertEquals("U2", result.getWinnerId());
            assertNotNull(result.getBidTimeMillis());

            verify(userDAO).updateReservedBalance(mockConn, "U1", -100.0);
            verify(userDAO).updateReservedBalance(mockConn, "U2", 210.0);

            ArgumentCaptor<BidTransaction> captor = ArgumentCaptor.forClass(BidTransaction.class);
            verify(bidDAO).insertBid(eq(mockConn), captor.capture());
            assertEquals(210.0, captor.getValue().getBidAmount());
            assertEquals("U2", captor.getValue().getBidderId());

            verify(mockConn).commit();
        }

        @Test
        void concurrentUpdateFailure_ReturnsFalse() throws SQLException {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            session.setCurrentPrice(100.0);

            AutoBid highest = new AutoBid();
            highest.setBidderId("U1");
            highest.setMaxBidAmount(200.0);

            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("SS-1"))).thenReturn(session);
            when(bidValidationService.isSessionCurrentlyBiddable(session)).thenReturn(true);
            when(autoBidDAO.getActiveAutoBidsBySessionSorted(any(Connection.class), eq("SS-1")))
                    .thenReturn(List.of(highest));
            when(bidIncrementService.getMinimumNextBid(anyDouble())).thenReturn(110.0);
            when(bidReservationCalculator.calculateReserveChange(any(), any(), anyDouble(), anyDouble())).thenReturn(110.0);
            when(userDAO.getBalanceForUpdate(any(Connection.class), eq("U1"))).thenReturn(new UserBalance( 1000.0, 0.0));

            when(sessionDAO.updateCurrentBid(any(Connection.class), anyString(), anyDouble(), anyString())).thenReturn(false);

            BidExecutionResult result = resolver.resolve("SS-1");

            assertFalse(result.isSuccess());
            assertEquals("Auto bid failed: session was updated by another request.", result.getMessage());
            verify(mockConn).rollback();
        }
    }
}