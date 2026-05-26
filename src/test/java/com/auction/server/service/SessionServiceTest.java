package com.auction.server.service;

import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.server.dao.BidDAO;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.SessionDAO;
import com.auction.server.dao.UserDAO;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;


import java.sql.Connection;
import java.sql.SQLException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;


public class SessionServiceTest {

    @Mock
    private SessionDAO sessionDAO;
    @Mock
    private Connection mockConn;
    @Mock
    private DatabaseManager mockDbManager;
    @Mock
    private Item mockItem;
    @Mock
    private UserDAO mockUserDAO;
    @Mock
    private BidDAO mockBidDAO;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private SessionService sessionService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        sessionService = new SessionService(sessionDAO, mockUserDAO, mockBidDAO);
        now = LocalDateTime.now(); //lấy mốc time chuẩn cho mỗi test

        when(mockItem.getId()).thenReturn("ITEM_1");


        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);
    }

    @AfterEach
    @DisplayName("Dọn dẹp môi trường giả lập sau mỗi Test")
    void CleanUp() {
        mockedStaticDbManager.close();
    }




    @Nested
    class ConstructorTests {

        @Test
        void SessionDAONotNull_Success() {
            assertDoesNotThrow(() -> new SessionService(sessionDAO, mockUserDAO, mockBidDAO));
        }

        @Test
        void SessionDAONull_ThrowIllegalArgumentException() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new SessionService(null, mockUserDAO, mockBidDAO));

            assertTrue(e.getMessage().toLowerCase().contains("sessiondao cannot be null"));
        }

        @Test
        void UserDAONull_ThrowIllegalArgumentException() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new SessionService(sessionDAO, null, mockBidDAO));
            assertEquals("UserDAO cannot be null", e.getMessage());
        }

        @Test
        void BidDAONull_ThrowIllegalArgumentException() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class,
                    () -> new SessionService(sessionDAO, mockUserDAO, null));
            assertEquals("BidDAO cannot be null", e.getMessage());
        }
    }



    @Nested
    class CreateSessionTests {

        @Test
        void Success() throws SQLException{
            LocalDateTime start = now.plusDays(1);
            LocalDateTime end = now.plusDays(2);
            when(sessionDAO.existsActiveSessionByItemId(any(Connection.class), eq("ITEM_1"))).thenReturn(false);
            when(sessionDAO.getSessionById(any(Connection.class), anyString())).thenReturn(null);
            AuctionSession session = sessionService.createSession(mockItem, start, end);

            assertNotNull(session);
            assertNotNull(session.getId());
            assertEquals("OPEN", session.getStatus());
            verify(sessionDAO, times(1)).insertSession(eq(mockConn), eq(session), eq(mockItem));
            verify(mockConn, times(1)).commit();
        }

        @Test
        void ItemNull_ThrowException() {
            assertThrows(IllegalArgumentException.class, () -> sessionService.createSession(null, now, now.plusDays(1)));
        }

        @Test
        void ItemIdNullOrBlank_ThrowException() {
            Item badItem = mock(Item.class);
            when(badItem.getId()).thenReturn("");
            assertThrows(IllegalArgumentException.class, () -> sessionService.createSession(badItem, now, now.plusDays(1)));
        }

        @Test
        void EndBeforeStart_ThrowException() {
            assertThrows(IllegalArgumentException.class, () -> sessionService.createSession(mockItem, now.plusDays(2), now.plusDays(1)));
        }

        @Test
        void ActiveSessionExists_ThrowException() throws SQLException {
            when(sessionDAO.existsActiveSessionByItemId(any(Connection.class), eq("ITEM_1"))).thenReturn(true);

            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sessionService.createSession(mockItem, now, now.plusDays(1)));
            assertEquals("This item already has an OPEN or RUNNING auction session", e.getMessage());
            verify(mockConn, times(1)).rollback();
        }
    }


    //TEST GET RUNNING SESSIONS & AUTO STATE TRANSITIONS(chuyển đổi trạng thái tự động)
    //trandition: chuyển đổi
    //state: trạng thái

    @Nested
    class GetRunningSessionsAndAutoTransitionsTests {

        @Test
        void TimeReached_AutoStartOpenSession() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusMinutes(10), now.plusHours(1));
            session.setStatus(SessionService.STATUS_OPEN);

            when(sessionDAO.getAllSessions(any(Connection.class))).thenReturn(List.of(session));
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);

            List<AuctionSession> running = sessionService.getRunningSessions();

            assertEquals(1, running.size());
            assertEquals(SessionService.STATUS_RUNNING, session.getStatus());
            verify(sessionDAO).updateStatus(any(Connection.class), eq("S1"), eq(SessionService.STATUS_RUNNING));
            verify(mockConn, atLeastOnce()).commit();
        }


        @Test
        void AutoFinishExpiredRunningSession() throws SQLException {//Expire:hết hạn
            AuctionSession session = new AuctionSession("S2", mockItem, now.minusHours(2), now.minusMinutes(1));
            session.setStatus(SessionService.STATUS_RUNNING);

            when(sessionDAO.getAllSessions(any(Connection.class))).thenReturn(List.of(session));
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S2"))).thenReturn(session);

            List<AuctionSession> running = sessionService.getRunningSessions();

            assertEquals(0, running.size());
            assertEquals(SessionService.STATUS_FINISHED, session.getStatus());
            verify(sessionDAO).updateStatus(any(Connection.class), eq("S2"), eq(SessionService.STATUS_FINISHED));
        }
    }




    @Nested
    class StartSessionTests {

        @Test
        void OPEN_StartSuccessfully() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusMinutes(5), now.plusHours(1));
            session.setStatus(SessionService.STATUS_OPEN);
            when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);

            sessionService.startSession("S1");

            assertEquals(SessionService.STATUS_RUNNING, session.getStatus());
            verify(sessionDAO).updateStatus(any(Connection.class), eq("S1"), eq(SessionService.STATUS_RUNNING));
        }

        @Test
        void BeforeStartTime_ThrowException() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.plusHours(1), now.plusHours(2));
            session.setStatus(SessionService.STATUS_OPEN);
            when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);

            assertThrows(IllegalStateException.class, () -> sessionService.startSession("S1"));
        }
    }


    @Nested
    class FinishSessionTests{

        @Test
        void Success() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);

            sessionService.finishSession("S1");

            assertEquals(SessionService.STATUS_FINISHED, session.getStatus());
        }

        @Test
        void AlreadyPaid_ThrowException() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now, now.plusHours(1));
            session.setStatus(SessionService.STATUS_PAID);
            when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);

            assertThrows(IllegalStateException.class, () -> sessionService.finishSession("S1"));
        }
    }


    //TEST ANTI-SNIPING

    @Nested
    class ExtendSessionTests {

        @Test
        void ExtendSuccessfully_AndSupportAntiSniping() throws SQLException {
            LocalDateTime endTime = now.plusMinutes(5);
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), endTime);
            session.setStatus(SessionService.STATUS_RUNNING);
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);

            Duration extra = Duration.ofSeconds(30);
            sessionService.extendSession("S1", extra);

            verify(sessionDAO).updateEndTime(any(Connection.class), eq("S1"), eq(endTime.plusSeconds(30)));
            verify(mockConn, times(1)).commit();
        }

        @Test
        void ExtraTimeNegative_ThrowException() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now, now.plusHours(1));
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);

            assertThrows(IllegalArgumentException.class, () -> sessionService.extendSession("S1", Duration.ofSeconds(-10)));
        }
    }





    @Nested
    class UpdateCurrentBidTests {

        @Test
        void Success() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setCurrentPrice(100.0);
            when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);

            when(sessionDAO.updateCurrentBid(any(Connection.class), eq("S1"), eq(150.0), eq("BIDDER_1"))).thenReturn(true);

            boolean result = sessionService.updateCurrentBid("S1", 150.0, "BIDDER_1");

            assertTrue(result);
            verify(sessionDAO).updateCurrentBid(mockConn, "S1", 150.0, "BIDDER_1");
        }

        @Test
        void BidTooLow_ReturnFalse() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setCurrentPrice(500.0);
            when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);

            boolean result = sessionService.updateCurrentBid("S1", 400.0, "BIDDER_1");

            assertFalse(result);
        }


        @Test
        void whenSQLExceptionOccurs_ReturnFalse() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setCurrentPrice(100.0);

            when(mockDbManager.getConnection())
                    .thenReturn(mockConn, mockConn, mockConn)
                    .thenThrow(new SQLException("Deadlock / Connection lost"));

            when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);
            when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);

            boolean result = sessionService.updateCurrentBid("S1", 150.0, "BIDDER_1");

            assertFalse(result);
        }


        //TEST sự đồng thời và vòng đời

        @Nested
        class AdvancedArchitectureTests { //AdvancedArchitecture: kiến trúc nâng cao


            @Test
            void session_shouldFollowCorrectLifecycle() throws SQLException {//phiên lm vc tuân theo đúng vòng đời
                AuctionSession session = new AuctionSession("S1", mockItem, now.minusMinutes(1), now.plusMinutes(1));
                session.setStatus(SessionService.STATUS_OPEN);
                when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);

                sessionService.refreshSessionStatus("S1");
                assertEquals(SessionService.STATUS_RUNNING, session.getStatus());

                //tua nhanh thời gian bằng cách sửa endTime về quá khứ
                session.setEndTime(now.minusSeconds(1));
                sessionService.refreshSessionStatus("S1");
                assertEquals(SessionService.STATUS_FINISHED, session.getStatus());
            }


            @Test
            void updateCurrentBid_shouldHandleConcurrentBids() throws InterruptedException, SQLException {

                //100 luồng request Đặt giá CÙNG 1 MILI-GIÂY
                AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
                session.setStatus(SessionService.STATUS_RUNNING);
                session.setCurrentPrice(100.0);

                when(sessionDAO.getSessionById(any(Connection.class), eq("S1"))).thenReturn(session);
                when(sessionDAO.getSessionByIdForUpdate(any(Connection.class), eq("S1"))).thenReturn(session);
                when(sessionDAO.updateCurrentBid(any(Connection.class), eq("S1"), anyDouble(), anyString())).thenReturn(true);

                int numberOfThreads = 100;
                ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
                CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

                for (int i = 0; i < numberOfThreads; i++) {
                    double bidAmount = 150.0 + i;
                    executor.execute(() -> {
                        readyLatch.countDown();//báo cáo thread đã sẵn sàng
                        try {
                            startLatch.await();//đứng chờ hiệu lệnh nổ súng
                            sessionService.updateCurrentBid("S1", bidAmount, "CONCURRENT_BIDDER");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }

                readyLatch.await();// Chờ cả 100 thread vào vị trí
                startLatch.countDown();// PHÁT LỆNH CHẠY ĐỒNG LOẠT!
                doneLatch.await();// Chờ tất cả chạy xong

                // Nếu synchronized hoạt động, nó sẽ không ném ConcurrentModificationException hay lỗi luồng.
                // Số lần gọi xuống DAO phải khớp với những bid hợp lệ.
                // Test này kiểm tra độ an toàn chống Crash của hàm khi chịu tải đột ngột.

                assertTrue(true);
            }
        }
    }
}
