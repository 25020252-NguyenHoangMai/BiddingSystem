package com.auction.server.service;

import com.auction.exception.InsufficientBalanceException;
import com.auction.model.AuctionSession;
import com.auction.model.BidTransaction;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

public class BidTransactionExecutorTest {

    @Mock private BidDAO bidDAO;
    @Mock private SessionDAO sessionDAO;
    @Mock private UserDAO userDAO;
    @Mock private BidValidationService bidValidationService;
    @Mock private BidReservationCalculator bidReservationCalculator;

    @Mock private Connection mockConn;
    @Mock private DatabaseManager mockDbManager;
    @Mock private AuctionSession mockSession;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private BidTransactionExecutor executor;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        executor = new BidTransactionExecutor(
                bidDAO, sessionDAO, userDAO, bidValidationService, bidReservationCalculator
        );


        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);
    }

    @AfterEach
    void cleanup() {
        mockedStaticDbManager.close();
    }


    @Nested
    class ConstructorTests {
        @Test
        void nullDependencies_ThrowsException() {//nullDependencies: truyenThamSoNull
            assertThrows(IllegalArgumentException.class, () -> new BidTransactionExecutor(null, sessionDAO, userDAO, bidValidationService, bidReservationCalculator));
            assertThrows(IllegalArgumentException.class, () -> new BidTransactionExecutor(bidDAO, null, userDAO, bidValidationService, bidReservationCalculator));
            assertThrows(IllegalArgumentException.class, () -> new BidTransactionExecutor(bidDAO, sessionDAO, null, bidValidationService, bidReservationCalculator));
            assertThrows(IllegalArgumentException.class, () -> new BidTransactionExecutor(bidDAO, sessionDAO, userDAO, null, bidReservationCalculator));
            assertThrows(IllegalArgumentException.class, () -> new BidTransactionExecutor(bidDAO, sessionDAO, userDAO, bidValidationService, null));
        }
    }


    @Nested
    class FailedTransactionTests { //FailedTransaction: giao dịch thất bại

        @BeforeEach
        void setupValidSession() throws SQLException {

            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenReturn(mockSession);
            when(mockSession.getId()).thenReturn("S1");
            when(mockSession.getCurrentPrice()).thenReturn(100.0);
            when(mockSession.getStatus()).thenReturn(SessionService.STATUS_RUNNING);
        }

        @Test
        void sessionNotFound_ReturnsFail_AndRollbacks() throws SQLException {

            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenReturn(null);

            BidExecutionResult result = executor.execute("S1", "B1", 150.0);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Auction session not found"));
            verify(mockConn).rollback();
        }

        @Test
        void sellerBidsOwnItem_ThrowsException_AndRollbacks() throws SQLException {//sellerBidsOwnItem: ng bán tự bid hàng của mk

            doThrow(new IllegalArgumentException("Seller cannot bid")).when(bidValidationService)
                    .validateSellerCannotBidOwnAuction(mockSession, "B1");

            assertThrows(IllegalArgumentException.class, () -> executor.execute("S1", "B1", 150.0));
            verify(mockConn).rollback();
        }

        @Test
        void sessionNotBiddable_ReturnsFail_AndRollbacks() throws SQLException {

            when(bidValidationService.isSessionCurrentlyBiddable(mockSession)).thenReturn(false);
            when(bidValidationService.buildNotBiddableMessage(mockSession)).thenReturn("Session closed");

            BidExecutionResult result = executor.execute("S1", "B1", 150.0);

            assertFalse(result.isSuccess());
            assertEquals("Session closed", result.getMessage());
            verify(mockConn).rollback();
        }

        @Test
        void reserveChangeNotPositive_ReturnsFail_AndRollbacks() throws SQLException {

            when(bidValidationService.isSessionCurrentlyBiddable(mockSession)).thenReturn(true);

            when(bidReservationCalculator.calculateReserveChange(any(), any(), anyDouble(), anyDouble())).thenReturn(0.0);

            BidExecutionResult result = executor.execute("S1", "B1", 100.0);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("must be higher than current price"));
            verify(mockConn).rollback();
        }

        @Test
        void insufficientBalance_ThrowsException_AndRollbacks() throws SQLException {//insufficientBalance :khongDuSoDu
            when(bidValidationService.isSessionCurrentlyBiddable(mockSession)).thenReturn(true);
            when(bidReservationCalculator.calculateReserveChange(any(), eq("B1"), eq(100.0), eq(150.0))).thenReturn(150.0);


            UserBalance lowBalance = new UserBalance(50.0, 0.0);
            when(userDAO.getBalanceForUpdate(mockConn, "B1")).thenReturn(lowBalance);

            assertThrows(InsufficientBalanceException.class, () -> executor.execute("S1", "B1", 150.0));
            verify(mockConn).rollback();
        }

        @Test
        void concurrentUpdateFails_ReturnsFail_AndRollbacks() throws SQLException {
            when(bidValidationService.isSessionCurrentlyBiddable(mockSession)).thenReturn(true);
            when(bidReservationCalculator.calculateReserveChange(any(), eq("B1"), eq(100.0), eq(150.0))).thenReturn(150.0);
            when(userDAO.getBalanceForUpdate(mockConn, "B1")).thenReturn(new UserBalance(1000.0, 0.0));


            when(sessionDAO.updateCurrentBid(mockConn, "S1", 150.0, "B1")).thenReturn(false);

            BidExecutionResult result = executor.execute("S1", "B1", 150.0);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("another higher bid was placed"));
            verify(mockConn).rollback();
        }

        @Test
        void databaseCrash_ThrowsRuntimeException_AndRollbacks() throws SQLException {

            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenThrow(new RuntimeException("Deadlock đột ngột"));

            RuntimeException e = assertThrows(RuntimeException.class, () -> executor.execute("S1", "B1", 150.0));
            assertTrue(e.getMessage().contains("Deadlock đột ngột"));

            verify(mockConn).rollback();
        }
    }


    @Nested
    class SuccessfulTransactionTests {

        @BeforeEach
        void setupSuccess() throws SQLException {
            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenReturn(mockSession);
            when(mockSession.getId()).thenReturn("S1");
            when(mockSession.getCurrentPrice()).thenReturn(100.0);
            when(mockSession.getStatus()).thenReturn(SessionService.STATUS_RUNNING);

            when(bidValidationService.isSessionCurrentlyBiddable(mockSession)).thenReturn(true);
            when(userDAO.getBalanceForUpdate(mockConn, "B1")).thenReturn(new UserBalance(1000.0, 0.0));
            when(sessionDAO.updateCurrentBid(mockConn, "S1", 150.0, "B1")).thenReturn(true);
        }

        @Test
        void firstBid_CommitsSuccessfully() throws SQLException {

            when(mockSession.getCurrentWinnerId()).thenReturn(null);

            when(bidReservationCalculator.calculateReserveChange(null, "B1", 100.0, 150.0)).thenReturn(150.0);

            BidExecutionResult result = executor.execute("S1", "B1", 150.0);

            assertTrue(result.isSuccess());

            verify(userDAO).updateReservedBalance(mockConn, "B1", 150.0);


            verify(bidDAO).insertBid(eq(mockConn), any(BidTransaction.class));

            verify(mockConn).commit();
            verify(mockConn).setAutoCommit(true);
        }


        @Test
        void outbidSomeone_RefundsOldWinner_AndCommitsSuccessfully() throws SQLException {

            when(mockSession.getCurrentWinnerId()).thenReturn("OLD_WINNER");

            when(bidReservationCalculator.calculateReserveChange("OLD_WINNER", "B1", 100.0, 150.0)).thenReturn(150.0);

            BidExecutionResult result = executor.execute("S1", "B1", 150.0);

            assertTrue(result.isSuccess());

            verify(userDAO).updateReservedBalance(mockConn, "OLD_WINNER", -100.0);

            verify(userDAO).updateReservedBalance(mockConn, "B1", 150.0);

            verify(mockConn).commit();
        }
    }
}