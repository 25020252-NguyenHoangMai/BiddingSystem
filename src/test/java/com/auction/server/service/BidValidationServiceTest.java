package com.auction.server.service;

import com.auction.exception.InvalidBidException;
import com.auction.exception.UserNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BidValidationServiceTest {

    @Mock private UserService userService;
    @Mock private BidIncrementService bidIncrementService;
    @Mock private AuctionSession session;
    @Mock private Item item;

    private BidValidationService validationService;

    @BeforeEach
    void setUp() {
        validationService = new BidValidationService(userService, bidIncrementService);
    }

    @Nested
    class ConstructorValidation {
        @Test
        void nullUserService_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new BidValidationService(null, bidIncrementService));
            assertEquals("UserService must not be null", ex.getMessage());
        }

        @Test
        void nullBidIncrementService_ThrowsException() {
            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> new BidValidationService(userService, null));
            assertEquals("BidIncrementService must not be null", ex.getMessage());
        }
    }

    @Nested
    class InputValidation {
        @Test
        void blankSessionId_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> validationService.validateBidInput("", "user1", 100.0));
        }

        @Test
        void blankBidderId_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> validationService.validateBidInput("ss1", "   ", 100.0));
        }

        @Test
        void negativeOrZeroAmount_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> validationService.validateBidInput("ss1", "user1", 0.0));
            assertThrows(IllegalArgumentException.class,
                    () -> validationService.validateBidInput("ss1", "user1", -50.0));
        }

        @Test
        void validInput_Success() {
            assertDoesNotThrow(() -> validationService.validateBidInput("ss1", "user1", 100.0));
        }
    }

    @Nested
    class RequireBidderValidation {
        @Test
        void validBidder_ReturnsBidder() {
            Bidder mockBidder = mock(Bidder.class);

            when(userService.requireActiveUserById("bidder1")).thenReturn(mockBidder);

            Bidder result = validationService.requireBidder("bidder1");
            assertNotNull(result);
        }

        @Test
        void notABidder_ThrowsException() {
            User mockNormalUser = mock(User.class);

            when(userService.requireActiveUserById("user1")).thenReturn(mockNormalUser);

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> validationService.requireBidder("user1"));
            assertEquals("Only bidder accounts can place bids.", ex.getMessage());
        }

        @Test
        void userNotFound_ThrowsException() {

            when(userService.requireActiveUserById("ghost")).thenThrow(new UserNotFoundException("Not found"));

            IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                    () -> validationService.requireBidder("ghost"));
            assertTrue(ex.getMessage().contains("Bidder not found"));
        }
    }

    @Nested
    class SessionBiddableValidation {
        @Test
        void isBiddable_True() {
            when(session.getStatus()).thenReturn("RUNNING");
            when(session.getStartTime()).thenReturn(LocalDateTime.now().minusHours(1));
            when(session.getEndTime()).thenReturn(LocalDateTime.now().plusHours(1));

            assertTrue(validationService.isSessionCurrentlyBiddable(session));
        }

        @Test
        void isBiddable_False_NotRunning() {
            when(session.getStatus()).thenReturn("OPEN");
            assertFalse(validationService.isSessionCurrentlyBiddable(session));
        }

        @Test
        void isBiddable_False_BeforeStartTime() {
            when(session.getStatus()).thenReturn("RUNNING");
            when(session.getStartTime()).thenReturn(LocalDateTime.now().plusHours(1));
            assertFalse(validationService.isSessionCurrentlyBiddable(session));
        }
    }

    @Nested
    class MessageBuilderValidation {
        @Test
        void buildMessages_ForAllStatuses() {
            when(session.getStatus()).thenReturn("OPEN");
            assertTrue(validationService.buildNotBiddableMessage(session).contains("not started yet"));

            when(session.getStatus()).thenReturn("FINISHED");
            assertTrue(validationService.buildNotBiddableMessage(session).contains("already finished"));

            when(session.getStatus()).thenReturn("CANCELED");
            assertTrue(validationService.buildNotBiddableMessage(session).contains("canceled"));

            when(session.getStatus()).thenReturn("PAID");
            assertTrue(validationService.buildNotBiddableMessage(session).contains("paid and closed"));

            when(session.getStatus()).thenReturn("UNKNOWN_STATUS");
            assertTrue(validationService.buildNotBiddableMessage(session).contains("not available for bidding"));
        }
    }

    @Nested
    class IncrementValidation {
        @Test
        void nullSession_ThrowsException() {
            assertThrows(IllegalArgumentException.class,
                    () -> validationService.validateBidIncrement(null, 100.0));
        }

        @Test
        void bidTooLow_ThrowsException() {
            when(session.getCurrentPrice()).thenReturn(100.0);
            when(bidIncrementService.getMinimumNextBid(100.0)).thenReturn(110.0);

            InvalidBidException ex = assertThrows(InvalidBidException.class,
                    () -> validationService.validateBidIncrement(session, 105.0)); // 105 < 110
            assertTrue(ex.getMessage().contains("minimum next bid is 110.0"));
        }

        @Test
        void validIncrement_Success() {
            when(session.getCurrentPrice()).thenReturn(100.0);
            when(bidIncrementService.getMinimumNextBid(100.0)).thenReturn(110.0);

            assertDoesNotThrow(() -> validationService.validateBidIncrement(session, 150.0));
        }
    }

    @Nested
    class SellerBiddingValidation {
        @Test
        void itemOrSellerNull_Success() {
            when(session.getItem()).thenReturn(null);
            assertDoesNotThrow(() -> validationService.validateSellerCannotBidOwnAuction(session, "user1"));
        }

        @Test
        void sellerBidsOwnItem_ThrowsException() {
            when(session.getItem()).thenReturn(item);
            when(item.getSellerId()).thenReturn("seller1");

            InvalidBidException ex = assertThrows(InvalidBidException.class,
                    () -> validationService.validateSellerCannotBidOwnAuction(session, "seller1"));
            assertTrue(ex.getMessage().contains("seller cannot bid on their own auction"));
        }

        @Test
        void normalBidder_Success() {
            when(session.getItem()).thenReturn(item);
            when(item.getSellerId()).thenReturn("seller1");

            assertDoesNotThrow(() -> validationService.validateSellerCannotBidOwnAuction(session, "bidder2"));
        }
    }
}