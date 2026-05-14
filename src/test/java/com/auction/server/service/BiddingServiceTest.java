package com.auction.server.service;

import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class BiddingServiceTest {

    @Mock private SessionService sessionService;
    @Mock private AntiSnipingService antiSnipingService;
    @Mock private UserService userService;
    @Mock private BidValidationService bidValidationService;
    @Mock private BidTransactionExecutor bidTransactionExecutor;

    private BiddingService biddingService;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        biddingService = new BiddingService(
                sessionService, antiSnipingService, userService, bidValidationService, bidTransactionExecutor
        );
    }

    @Nested
    class ConstructorTests {
        @Test
        void success() {
            assertDoesNotThrow(() -> new BiddingService(sessionService, antiSnipingService, userService, bidValidationService, bidTransactionExecutor));
        }

        @Test
        void nullDependencies_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(null, antiSnipingService, userService, bidValidationService, bidTransactionExecutor));
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, null, userService, bidValidationService, bidTransactionExecutor));
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, antiSnipingService, null, bidValidationService, bidTransactionExecutor));
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, antiSnipingService, userService, null, bidTransactionExecutor));
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, antiSnipingService, userService, bidValidationService, null));
        }
    }

    @Nested
    class ValidationTests {
        @Test
        void validateBidInput_ThrowsException() {
            doThrow(new IllegalArgumentException("Invalid Input")).when(bidValidationService).validateBidInput(anyString(), anyString(), anyDouble());
            assertThrows(IllegalArgumentException.class, () -> biddingService.placeBid("S1", "B1", 100));
            verify(sessionService, never()).refreshSessionStatus(anyString());
        }

        @Test
        void requireBidder_ThrowsException() {
            doThrow(new IllegalArgumentException("Not a bidder")).when(bidValidationService).requireBidder(anyString());
            assertThrows(IllegalArgumentException.class, () -> biddingService.placeBid("S1", "B1", 100));
            verify(sessionService, never()).refreshSessionStatus(anyString());
        }
    }

    @Nested
    class TransactionFailAndResolveWinnerTests {

        private AuctionSession mockSession;

        @BeforeEach
        void setupSession() {

            mockSession = new AuctionSession();
            mockSession.setId("S1");
            mockSession.setCurrentWinnerId("OLD_WINNER");
            when(sessionService.getSession("S1")).thenReturn(mockSession);
        }

        @Test
        void fail_WinnerIdNull_ReturnsNullUsername() {
            BidExecutionResult failResult = new BidExecutionResult(false, "Fail", "S1", 100, null, "RUNNING");
            when(bidTransactionExecutor.execute("S1", "B1", 150)).thenReturn(failResult);

            BidResult result = biddingService.placeBid("S1", "B1", 150);

            assertFalse(result.isSuccess());
            assertNull(result.getCurrentWinnerUsername());
            verify(userService, never()).getUserById(anyString());
        }

        @Test
        void fail_WinnerIdBlank_ReturnsNullUsername() {
            BidExecutionResult failResult = new BidExecutionResult(false, "Fail", "S1", 100, "   ", "RUNNING");
            when(bidTransactionExecutor.execute("S1", "B1", 150)).thenReturn(failResult);

            BidResult result = biddingService.placeBid("S1", "B1", 150);

            assertNull(result.getCurrentWinnerUsername());
            verify(userService, never()).getUserById(anyString());
        }

        @Test
        void fail_WinnerExists_ReturnsUsername() throws UserNotFoundException {
            BidExecutionResult failResult = new BidExecutionResult(false, "Fail", "S1", 100, "U99", "RUNNING");
            when(bidTransactionExecutor.execute("S1", "B1", 150)).thenReturn(failResult);

            User winner = new Bidder("U99", "winner_name", "pass", "Name", "BIDDER", 0, 0);
            when(userService.getUserById("U99")).thenReturn(winner);

            BidResult result = biddingService.placeBid("S1", "B1", 150);

            assertEquals("winner_name", result.getCurrentWinnerUsername());
        }

        @Test
        void fail_UserNotFoundException_ReturnsNullUsername() throws UserNotFoundException {
            BidExecutionResult failResult = new BidExecutionResult(false, "Fail", "S1", 100, "GHOST", "RUNNING");
            when(bidTransactionExecutor.execute("S1", "B1", 150)).thenReturn(failResult);

            when(userService.getUserById("GHOST")).thenThrow(new UserNotFoundException("Not found"));

            BidResult result = biddingService.placeBid("S1", "B1", 150);

            assertNull(result.getCurrentWinnerUsername());
        }
    }

    @Nested
    class TransactionSuccessTests {

        private AuctionSession updatedSession;
        private AuctionSession initialSession;

        @BeforeEach
        void setupSuccess() throws UserNotFoundException {
            BidExecutionResult successResult = new BidExecutionResult(true, "OK", "S1", 150, "B1", "RUNNING");
            when(bidTransactionExecutor.execute("S1", "B1", 150)).thenReturn(successResult);


            User dummyUser = new Bidder("B1", "Minh_UET", "pass", "Name", "BIDDER", 0, 0);
            when(userService.getUserById("B1")).thenReturn(dummyUser);


            initialSession = new AuctionSession();
            initialSession.setId("S1");
            initialSession.setCurrentWinnerId("OLD_WINNER");


            updatedSession = new AuctionSession();
            updatedSession.setId("S1");
            updatedSession.setCurrentPrice(150.0);
            updatedSession.setCurrentWinnerId("B1");
        }

        @Test
        void success_ReloadSessionNull_ReturnsPartialResult() {

            when(sessionService.getSession("S1")).thenReturn(initialSession, null);

            BidResult result = biddingService.placeBid("S1", "B1", 150);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("failed to reload updated session"));
        }

        @Test
        void success_NoAntiSniping() {

            when(sessionService.getSession("S1")).thenReturn(initialSession, updatedSession);
            when(antiSnipingService.shouldExtend(updatedSession)).thenReturn(false);

            BidResult result = biddingService.placeBid("S1", "B1", 150);

            assertTrue(result.isSuccess());
            assertEquals("Bid placed successfully", result.getMessage());
            verify(sessionService, never()).extendSession(anyString(), any());
        }

        @Test
        void success_WithAntiSniping_ExtendsAndReloadsAgain() {

            when(sessionService.getSession("S1")).thenReturn(initialSession, updatedSession, updatedSession);
            when(antiSnipingService.shouldExtend(updatedSession)).thenReturn(true);
            when(antiSnipingService.getExtendTime()).thenReturn(Duration.ofSeconds(60));

            BidResult result = biddingService.placeBid("S1", "B1", 150);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("extended due to anti-sniping"));

            verify(sessionService).extendSession("S1", Duration.ofSeconds(60));
            verify(sessionService, times(3)).getSession("S1");
        }
    }

    @Nested
    class VerifyInteractionTests {
        @Test
        void verifyMethodCallOrderAndParameters() {

            AuctionSession mockSession = new AuctionSession();
            mockSession.setId("S1");
            when(sessionService.getSession("S1")).thenReturn(mockSession);

            BidExecutionResult failResult = new BidExecutionResult(false, "Fail", "S1", 100, null, "RUNNING");
            when(bidTransactionExecutor.execute("S1", "B1", 150)).thenReturn(failResult);

            biddingService.placeBid("S1", "B1", 150);

            InOrder inOrder = inOrder(bidValidationService, sessionService, bidTransactionExecutor);

            inOrder.verify(bidValidationService).validateBidInput("S1", "B1", 150);
            inOrder.verify(bidValidationService).requireBidder("B1");
            inOrder.verify(sessionService).refreshSessionStatus("S1");
            inOrder.verify(sessionService).getSession("S1"); // Chèn thêm bước lấy session vào luồng Order
            inOrder.verify(bidTransactionExecutor).execute("S1", "B1", 150);
        }
    }
}