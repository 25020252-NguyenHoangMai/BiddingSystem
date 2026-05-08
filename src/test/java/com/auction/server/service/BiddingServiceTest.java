package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.InsufficientBalanceException;
import com.auction.exception.InvalidBidException;
import com.auction.exception.UserNotFoundException;
import com.auction.model.Admin;
import com.auction.model.AuctionSession;
import com.auction.model.BidTransaction;
import com.auction.model.Bidder;
import com.auction.server.dao.*;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

public class BiddingServiceTest {

    @Mock private SessionService sessionService;
    @Mock private BidDAO bidDAO;
    @Mock private AntiSnipingService antiSnipingService;
    @Mock private UserService userService;
    @Mock private BidIncrementService bidIncrementService;
    @Mock private SessionDAO sessionDAO;
    @Mock private UserDAO userDAO;

    @Mock private Connection mockConn;
    @Mock private DatabaseManager mockDbManager;
    @Mock private AuctionSession mockSession;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private BiddingService biddingService;
    private Bidder validBidder;
    private LocalDateTime now;

    @BeforeEach
    void setUp() throws SQLException, UserNotFoundException {
        MockitoAnnotations.openMocks(this);
        biddingService = new BiddingService(sessionService, bidDAO, antiSnipingService, userService,
                bidIncrementService, sessionDAO, userDAO);
        now = LocalDateTime.now();

        //giả lập Database Connection
        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);

        //giả lập Bidder hợp lệ
        validBidder = new Bidder("B1", "user1", "pass", "Name", "BIDDER", 1000.0, 0.0);
        when(userService.getUserById("B1")).thenReturn(validBidder);
    }

    @AfterEach
    void tearDown() {
        mockedStaticDbManager.close();
    }


    //TEST CONSTRUCTOR BẢO VỆ DEPENDENCY

    @Nested
    class ConstructorTests {
        @Test
        void constructor_shouldCreateSuccessfully() {
            assertDoesNotThrow(() -> new BiddingService(sessionService, bidDAO, antiSnipingService,
                    userService, bidIncrementService, sessionDAO, userDAO));
        }

        @Test
        void constructor_shouldThrowExceptions_whenDependenciesNull() {
            //thiếu SessionService
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(null, bidDAO, antiSnipingService, userService, bidIncrementService, sessionDAO, userDAO));

            //thiếu BidDAO
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, null, antiSnipingService, userService, bidIncrementService, sessionDAO, userDAO));

            //thiếu AntiSnipingService
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, bidDAO, null, userService, bidIncrementService, sessionDAO, userDAO));

            //thiếu UserService
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, bidDAO, antiSnipingService, null, bidIncrementService, sessionDAO, userDAO));

            //thiếu BidIncrementService
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, bidDAO, antiSnipingService, userService, null, sessionDAO, userDAO));

            //thiếu SessionDAO
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, bidDAO, antiSnipingService, userService, bidIncrementService, null, userDAO));

            //thiếu UserDAO
            assertThrows(IllegalArgumentException.class, () -> new BiddingService(sessionService, bidDAO, antiSnipingService, userService, bidIncrementService, sessionDAO, null));
        }
    }


    //TEST VALIDATION ĐẦU VÀO & PHÂN QUYỀN

    @Nested
    class ValidationTests {
        @Test
        void placeBid_shouldThrowException_whenInputInvalid() {
            assertThrows(IllegalArgumentException.class, () -> biddingService.placeBid("", "B1", 100));
            assertThrows(IllegalArgumentException.class, () -> biddingService.placeBid("S1", "", 100));
            assertThrows(IllegalArgumentException.class, () -> biddingService.placeBid("S1", "B1", -10));
        }

        @Test
        void placeBid_shouldThrowException_whenUserNotFound() throws UserNotFoundException {
            when(userService.getUserById("GHOST")).thenThrow(new UserNotFoundException("Not found"));
            assertThrows(IllegalArgumentException.class, () -> biddingService.placeBid("S1", "GHOST", 100));
        }

        @Test
        void placeBid_shouldThrowException_whenUserIsAdmin() throws UserNotFoundException {
            Admin admin = new Admin("A1", "admin", "pass", "Admin");
            when(userService.getUserById("A1")).thenReturn(admin);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> biddingService.placeBid("S1", "A1", 100));
            assertEquals("Only bidder accounts can place bids.", e.getMessage());
        }
    }


    //TEST CÁC NHÁNH LỖI GÂY ROLLBACK DATABASE

    @Nested
    class TransactionRollbackTests {

        @BeforeEach
        void setupValidSession() throws SQLException {
            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenReturn(mockSession);
            when(mockSession.getId()).thenReturn("S1");
            when(mockSession.getStatus()).thenReturn(SessionService.STATUS_RUNNING);
            when(mockSession.getStartTime()).thenReturn(now.minusHours(1));
            when(mockSession.getEndTime()).thenReturn(now.plusHours(1));
            when(mockSession.getCurrentPrice()).thenReturn(100.0);
        }

        @Test
        void placeBid_SessionNotFound_RollbacksAndReturnsFalse() throws SQLException {
            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenReturn(null); //giả lập khóa thất bại

            BidResult result = biddingService.placeBid("S1", "B1", 150.0);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("Auction session not found"));
            verify(mockConn).rollback(); // Phải Rollback!
        }

        @Test
        void placeBid_SessionNotRunning_RollbacksAndReturnsFalse() throws SQLException {
            when(mockSession.getStatus()).thenReturn(SessionService.STATUS_FINISHED);

            BidResult result = biddingService.placeBid("S1", "B1", 150.0);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("already finished"));
            verify(mockConn).rollback();
        }

        @Test
        void placeBid_InvalidBidIncrement_ThrowsExceptionAndRollbacks() throws SQLException {
            //giả lập mức giá tối thiểu phải là 150 nhưng user đặt 120
            when(bidIncrementService.getMinimumNextBid(100.0)).thenReturn(150.0);

            InvalidBidException e = assertThrows(InvalidBidException.class, () -> biddingService.placeBid("S1", "B1", 120.0));
            assertTrue(e.getMessage().contains("minimum next bid is 150.0"));
            verify(mockConn).rollback();
        }

        @Test
        void placeBid_InsufficientBalance_ThrowsExceptionAndRollbacks() throws SQLException {
            when(bidIncrementService.getMinimumNextBid(100.0)).thenReturn(150.0);

            //giả lập trong ví chỉ còn 50k có thể dùng nhưng định đặt 150k
            UserBalance lowBalance = new UserBalance(50.0, 0.0);
            when(userDAO.getBalanceForUpdate(mockConn, "B1")).thenReturn(lowBalance);

            InsufficientBalanceException e = assertThrows(InsufficientBalanceException.class, () -> biddingService.placeBid("S1", "B1", 150.0));
            assertTrue(e.getMessage().contains("Insufficient available balance"));
            verify(mockConn).rollback();
        }

        @Test
        void placeBid_ConcurrentUpdateConflict_RollbacksAndReturnsFalse() throws SQLException {
            when(bidIncrementService.getMinimumNextBid(100.0)).thenReturn(150.0);
            when(userDAO.getBalanceForUpdate(mockConn, "B1")).thenReturn(new UserBalance(1000.0, 0.0));

            //giả lập lúc update DB thì có luồng khác chen ngang đổi giá
            when(sessionDAO.updateCurrentBid(mockConn, "S1", 150.0, "B1")).thenReturn(false);

            BidResult result = biddingService.placeBid("S1", "B1", 150.0);

            assertFalse(result.isSuccess());
            assertTrue(result.getMessage().contains("another higher bid was placed"));
            verify(mockConn).rollback();
        }

        @Test
        void placeBid_DatabaseCrash_RollbacksAndThrowsAuctionException() throws SQLException {

            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenThrow(new AuctionException("Deadlock"));

            AuctionException e = assertThrows(AuctionException.class, () -> biddingService.placeBid("S1", "B1", 150.0));

            // Verify đảm bảo hệ thống có gọi rollback để bảo toàn tiền bạc
            verify(mockConn).rollback();
            //lỗi RuntimeException sẽ bị ném ra
            assertTrue(e.getMessage().contains("Deadlock"));
        }
    }


    //TEST LUỒNG THÀNH CÔNG

    @Nested
    class SuccessfulBidTests {

        @BeforeEach
        void setupValidScenario() throws SQLException {
            when(sessionDAO.getSessionByIdForUpdate(mockConn, "S1")).thenReturn(mockSession);
            when(mockSession.getId()).thenReturn("S1");
            when(mockSession.getStatus()).thenReturn(SessionService.STATUS_RUNNING);
            when(mockSession.getStartTime()).thenReturn(now.minusHours(1));
            when(mockSession.getEndTime()).thenReturn(now.plusHours(1));
            when(mockSession.getCurrentPrice()).thenReturn(100.0);

            when(bidIncrementService.getMinimumNextBid(100.0)).thenReturn(110.0);
            when(userDAO.getBalanceForUpdate(mockConn, "B1")).thenReturn(new UserBalance(1000.0, 0.0));
            when(sessionDAO.updateCurrentBid(mockConn, "S1", 150.0, "B1")).thenReturn(true);

            //giả lập load lại session sau khi Bid thành công
            when(sessionService.getSession("S1")).thenReturn(mockSession);
        }

        @Test
        void placeBid_FirstBid_CommitsSuccessfully() throws SQLException {
            //chưa có ai bid trc đó
            when(mockSession.getCurrentWinnerId()).thenReturn(null);

            BidResult result = biddingService.placeBid("S1", "B1", 150.0);

            assertTrue(result.isSuccess());
            assertEquals("Bid placed successfully", result.getMessage());

            //đảm bảo đóng băng tiền và ghi lsử Bid
            verify(userDAO).updateReservedBalance(mockConn, "B1", 150.0);
            verify(bidDAO).insertBid(eq(mockConn), any(BidTransaction.class));
            verify(mockConn).commit(); // THẮNG LỢI: Lệnh commit đã được gọi!
        }

        @Test
        void placeBid_OutbidSomeoneElse_ReleasesOldReserveAndCommits() throws Exception {
            //đã có người bid trc là U99 với giá 100k
            when(mockSession.getCurrentWinnerId()).thenReturn("U99");

            // cho Mockito bt U99 là ai để ko bị văng NullPointerException
            Bidder oldWinner = new Bidder("U99", "old_user", "pass", "Old Name", "BIDDER", 1000.0, 100.0);
            when(userService.getUserById("U99")).thenReturn(oldWinner);

            BidResult result = biddingService.placeBid("S1", "B1", 150.0);

            assertTrue(result.isSuccess());
            //giải phóng 100k tiền đặt cọc của U99
            verify(userDAO).updateReservedBalance(mockConn, "U99", -100.0);
            //đóng băng 150k tiền đặt cọc của B1
            verify(userDAO).updateReservedBalance(mockConn, "B1", 150.0);
            verify(mockConn).commit();
        }

        @Test
        void placeBid_TriggerAntiSniping_CommitsAndExtendsTime() throws SQLException {
            when(mockSession.getCurrentWinnerId()).thenReturn(null);

            //giả lập phút chót, kích hoạt Antisniping
            when(antiSnipingService.shouldExtend(mockSession)).thenReturn(true);
            when(antiSnipingService.getExtendTime()).thenReturn(Duration.ofSeconds(60));

            BidResult result = biddingService.placeBid("S1", "B1", 150.0);

            assertTrue(result.isSuccess());
            assertTrue(result.getMessage().contains("extended due to anti-sniping"));

            //đảm bảo Service có gọi hàm gia hạn time
            verify(sessionService).extendSession("S1", Duration.ofSeconds(60));
            verify(mockConn).commit();
        }
    }
}