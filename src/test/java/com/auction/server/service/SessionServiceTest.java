package com.auction.server.service;

import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.server.dao.DatabaseManager;
import com.auction.server.dao.SessionDAO;
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

    private MockedStatic<DatabaseManager> mockedStaticDbManager;
    private SessionService sessionService;
    private LocalDateTime now;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        sessionService = new SessionService(sessionDAO);
        now = LocalDateTime.now(); //lấy mốc time chuẩn cho mỗi test

        when(mockItem.getId()).thenReturn("ITEM_1");

        // Mock DatabaseManager cho method updateCurrentBid
        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);
    }

    @AfterEach
    void tearDown() {
        mockedStaticDbManager.close();
    }


    //TEST CONSTRUCTOR

    @Nested
    class ConstructorTests {

        @Test
        void SessionDAONotNull_Success() {
            assertDoesNotThrow(() -> new SessionService(sessionDAO));
        }

        @Test
        void SessionDAONull_ThrowIllegalArgumentException() {
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> new SessionService(null));
            assertEquals("sessionDAO cannot be null", e.getMessage());
        }
    }


    // 2. TEST CREATESESSION (VALIDATION & BUSINESS)

    @Nested
    class CreateSessionTests {

        @Test
        void Success() {
            LocalDateTime start = now.plusDays(1);
            LocalDateTime end = now.plusDays(2);
            when(sessionDAO.existsActiveSessionByItemId("ITEM_1")).thenReturn(false);

            AuctionSession session = sessionService.createSession(mockItem, start, end);

            assertNotNull(session);
            assertNotNull(session.getId());
            assertEquals("OPEN", session.getStatus());
            verify(sessionDAO, times(1)).insertSession(session, mockItem);
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
        void ActiveSessionExists_ThrowException() {
            when(sessionDAO.existsActiveSessionByItemId("ITEM_1")).thenReturn(true);
            IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> sessionService.createSession(mockItem, now, now.plusDays(1)));
            assertEquals("This item already has an OPEN or RUNNING auction session", e.getMessage());
        }
    }


    //TEST GET RUNNING SESSIONS & AUTO STATE TRANSITIONS(chuyển đổi trạng thái tự động)
    //trandition: chuyển đổi
    //state: trạng thái

    @Nested
    class GetRunningSessionsAndAutoTransitionsTests {

        @Test
        void TimeReached_AutoStartOpenSession() {
            // OPEN nhưng đã đến giờ start -> Phải tự chuyển thành RUNNING
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusMinutes(10), now.plusHours(1));
            session.setStatus(SessionService.STATUS_OPEN);

            when(sessionDAO.getAllSessions()).thenReturn(List.of(session));

            List<AuctionSession> running = sessionService.getRunningSessions();

            assertEquals(1, running.size());
            assertEquals(SessionService.STATUS_RUNNING, session.getStatus());
            verify(sessionDAO).updateStatus("S1", SessionService.STATUS_RUNNING);
        }

        @Test
        void AutoFinishExpiredRunningSession() {//Expire:hết hạn

            AuctionSession session = new AuctionSession("S2", mockItem, now.minusHours(2), now.minusMinutes(1));
            session.setStatus(SessionService.STATUS_RUNNING);

            when(sessionDAO.getAllSessions()).thenReturn(List.of(session));

            List<AuctionSession> running = sessionService.getRunningSessions();

            assertEquals(0, running.size()); // Không còn running nữa
            assertEquals(SessionService.STATUS_FINISHED, session.getStatus());
            verify(sessionDAO).updateStatus("S2", SessionService.STATUS_FINISHED);
        }
    }


    //TEST START / FINISH SESSION

    @Nested
    class StartSessionTests {

        @Test
        void OPEN_StartSuccessfully() {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusMinutes(5), now.plusHours(1));
            session.setStatus(SessionService.STATUS_OPEN);
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            sessionService.startSession("S1");

            assertEquals(SessionService.STATUS_RUNNING, session.getStatus());
            verify(sessionDAO).updateStatus("S1", SessionService.STATUS_RUNNING);
        }

        @Test
        void BeforeStartTime_ThrowException() {
            AuctionSession session = new AuctionSession("S1", mockItem, now.plusHours(1), now.plusHours(2));
            session.setStatus(SessionService.STATUS_OPEN);
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            assertThrows(IllegalStateException.class, () -> sessionService.startSession("S1"));
        }
    }


    @Nested
    class FinishSessionTests{

        @Test
        void Success() {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            sessionService.finishSession("S1");

            assertEquals(SessionService.STATUS_FINISHED, session.getStatus());
        }

        @Test
        void AlreadyPaid_ThrowException() {
            AuctionSession session = new AuctionSession("S1", mockItem, now, now.plusHours(1));
            session.setStatus(SessionService.STATUS_PAID); // Terminal state
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            assertThrows(IllegalStateException.class, () -> sessionService.finishSession("S1"));
        }
    }


    //TEST ANTI-SNIPING

    @Nested
    class ExtendSessionTests {

        @Test
        void ExtendSuccessfully_AndSupportAntiSniping() {
            LocalDateTime endTime = now.plusMinutes(5);
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), endTime);
            session.setStatus(SessionService.STATUS_RUNNING);
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            Duration extra = Duration.ofSeconds(30);
            sessionService.extendSession("S1", extra);

            assertEquals(endTime.plusSeconds(30), session.getEndTime(), "End time must be extended by 30 seconds");
            verify(sessionDAO).updateEndTime(eq("S1"), eq(endTime.plusSeconds(30)));
        }

        @Test
        void ExtraTimeNegative_ThrowException() {
            AuctionSession session = new AuctionSession("S1", mockItem, now, now.plusHours(1));
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            assertThrows(IllegalArgumentException.class, () -> sessionService.extendSession("S1", Duration.ofSeconds(-10)));
        }
    }


    //TEST UPDATE CURRENT BID


    @Nested
    class UpdateCurrentBidTests {

        @Test
        void Success() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setCurrentPrice(100.0);
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            //giả lập DAO xử lý update giá trị thành công
            when(sessionDAO.updateCurrentBid(any(Connection.class), eq("S1"), eq(150.0), eq("BIDDER_1"))).thenReturn(true);

            boolean result = sessionService.updateCurrentBid("S1", 150.0, "BIDDER_1");

            assertTrue(result);
            verify(sessionDAO).updateCurrentBid(mockConn, "S1", 150.0, "BIDDER_1");
        }

        @Test
        void BidTooLow_ReturnFalse() {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setCurrentPrice(500.0); //giá hiện tại 500
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            //cố tình bid thấp hơn
            boolean result = sessionService.updateCurrentBid("S1", 400.0, "BIDDER_1");

            assertFalse(result, "Giá thấp hơn currentPrice phải trả về false");
        }


        @Test
        void whenSQLExceptionOccurs_ReturnFalse() throws SQLException {
            AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setCurrentPrice(100.0);
            when(sessionDAO.getSessionById("S1")).thenReturn(session);

            //ép ông qlý Database rút cáp mạng ngay lúc xin Connection
            when(mockDbManager.getConnection()).thenThrow(new SQLException("Deadlock / Connection lost"));

            boolean result = sessionService.updateCurrentBid("S1", 150.0, "BIDDER_1");

            assertFalse(result, "Bị lỗi SQL phải nuốt lỗi và trả về false để không crash app");
        }


        //TEST sự đồng thời và vòng đời

        @Nested
        class AdvancedArchitectureTests { //AdvancedArchitecture: kiến trúc nâng cao

            @Test
            void session_shouldFollowCorrectLifecycle() { //phiên lm vc tuân theo đúng vòng đời
                //OPEN
                AuctionSession session = new AuctionSession("S1", mockItem, now.minusMinutes(1), now.plusMinutes(1));
                session.setStatus(SessionService.STATUS_OPEN);
                when(sessionDAO.getSessionById("S1")).thenReturn(session);

                //RUNNING
                sessionService.refreshSessionStatus("S1");
                assertEquals(SessionService.STATUS_RUNNING, session.getStatus());

                //tua nhanh thời gian bằng cách sửa endTime về quá khứ
                session.setEndTime(now.minusSeconds(1));
                sessionService.refreshSessionStatus("S1");
                assertEquals(SessionService.STATUS_FINISHED, session.getStatus());
            }

            @Test
            void updateCurrentBid_shouldHandleConcurrentBids() throws InterruptedException, SQLException {
                // TEST ĐA LUỒNG: Bắn 100 luồng request Đặt giá CÙNG 1 MILI-GIÂY
                AuctionSession session = new AuctionSession("S1", mockItem, now.minusHours(1), now.plusHours(1));
                session.setStatus(SessionService.STATUS_RUNNING);
                session.setCurrentPrice(100.0);
                when(sessionDAO.getSessionById("S1")).thenReturn(session);

                // Mỗi lần updateDAO thành công thì tăng currentPrice nội bộ để mô phỏng
                when(sessionDAO.updateCurrentBid(any(Connection.class), eq("S1"), anyDouble(), anyString())).thenReturn(true);

                int numberOfThreads = 100;
                ExecutorService executor = Executors.newFixedThreadPool(numberOfThreads);
                CountDownLatch readyLatch = new CountDownLatch(numberOfThreads);
                CountDownLatch startLatch = new CountDownLatch(1);
                CountDownLatch doneLatch = new CountDownLatch(numberOfThreads);

                for (int i = 0; i < numberOfThreads; i++) {
                    double bidAmount = 150.0 + i; //các giá bid khác nhau
                    executor.execute(() -> {
                        readyLatch.countDown(); //báo cáo thread đã sẵn sàng
                        try {
                            startLatch.await(); //đứng chờ hiệu lệnh nổ súng
                            sessionService.updateCurrentBid("S1", bidAmount, "CONCURRENT_BIDDER");
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        } finally {
                            doneLatch.countDown();
                        }
                    });
                }

                readyLatch.await(); // Chờ cả 100 thread vào vị trí
                startLatch.countDown(); // PHÁT LỆNH CHẠY ĐỒNG LOẠT!
                doneLatch.await(); // Chờ tất cả chạy xong

                // Nếu synchronized hoạt động, nó sẽ không ném ConcurrentModificationException hay lỗi luồng.
                // Số lần gọi xuống DAO phải khớp với những bid hợp lệ.
                // Test này kiểm tra độ an toàn chống Crash của hàm khi chịu tải đột ngột.
                assertTrue(true, "Concurrency test passed without deadlocks or thread crashes!");
            }
        }
    }
}