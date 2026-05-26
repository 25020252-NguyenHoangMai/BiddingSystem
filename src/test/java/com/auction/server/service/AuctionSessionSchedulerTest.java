package com.auction.server.service;

import com.auction.model.AuctionSession;
import com.auction.model.User;
import com.auction.response.BidUpdateResponse;

import com.auction.server.realtime.SessionWatchRegistry;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.lang.reflect.Method;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class AuctionSessionSchedulerTest {

    @Mock private SessionService sessionService;
    @Mock private SessionWatchRegistry sessionWatchRegistry;
    @Mock private DashboardRealtimeService dashboardRealtimeService;
    @Mock private UserService userService;

    private AuctionSessionScheduler scheduler;
    private Method runOnceMethod;

    @BeforeEach
    void setUp() throws Exception {
        MockitoAnnotations.openMocks(this);
        scheduler = new AuctionSessionScheduler(sessionService, sessionWatchRegistry, dashboardRealtimeService, userService);

        runOnceMethod = AuctionSessionScheduler.class.getDeclaredMethod("runOnce");
        runOnceMethod.setAccessible(true);
    }

    @AfterEach
    void Cleanup() {
        scheduler.stop();
    }

    @Nested
    class LifecycleTests {
        @Test
        void startAndStop_ExecutesWithoutExceptions() {
            assertDoesNotThrow(() -> scheduler.start());
            assertDoesNotThrow(() -> scheduler.stop());
        }
    }

    @Nested
    class RunOnceLogicTests {

        @Test
        void noDueOrExpiredSessions_DoesNothing() throws Exception {
            when(sessionService.startDueSessions()).thenReturn(Collections.emptyList());
            when(sessionService.finalizeExpiredSessions()).thenReturn(null);

            runOnceMethod.invoke(scheduler);

            verify(sessionWatchRegistry, never()).broadcastBidUpdate(anyString(), any());
            verify(dashboardRealtimeService, never()).broadcastItemUpdatedBySessionId(anyString(), anyString());
        }

        @Test
        void startDueSessions_BroadcastsSuccessfully() throws Exception {
            AuctionSession session = new AuctionSession();
            session.setId("SS-1");
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setCurrentPrice(100.0);
            session.setEndTime(LocalDateTime.now().plusDays(1));

            when(sessionService.startDueSessions()).thenReturn(List.of(session));
            when(sessionService.finalizeExpiredSessions()).thenReturn(Collections.emptyList());

            runOnceMethod.invoke(scheduler);

            verify(dashboardRealtimeService).broadcastItemUpdatedBySessionId("SS-1", "Auction started");

            ArgumentCaptor<BidUpdateResponse> captor = ArgumentCaptor.forClass(BidUpdateResponse.class);
            verify(sessionWatchRegistry).broadcastBidUpdate(eq("SS-1"), captor.capture());

            assertTrue(captor.getValue().isSuccess());
            assertEquals("Auction started", captor.getValue().getMessage());
            assertEquals("RUNNING", captor.getValue().getStatus());
            assertNotNull(captor.getValue().getEndTimeMillis());
        }

        @Test
        void finalizeExpiredSessions_WithWinner_BroadcastsSuccessfully() throws Exception {
            AuctionSession session = new AuctionSession();
            session.setId("SS-2");
            session.setStatus(SessionService.STATUS_PAID);
            session.setCurrentWinnerId("U1");

            User winner = mock(User.class);
            when(winner.getUsername()).thenReturn("nguyen_cong_minh");

            when(sessionService.startDueSessions()).thenReturn(Collections.emptyList());
            when(sessionService.finalizeExpiredSessions()).thenReturn(List.of(session));
            when(userService.getUserById("U1")).thenReturn(winner);

            runOnceMethod.invoke(scheduler);

            verify(dashboardRealtimeService).broadcastItemUpdatedBySessionId("SS-2", "Auction finalized");

            ArgumentCaptor<BidUpdateResponse> captor = ArgumentCaptor.forClass(BidUpdateResponse.class);
            verify(sessionWatchRegistry).broadcastBidUpdate(eq("SS-2"), captor.capture());

            assertEquals("nguyen_cong_minh", captor.getValue().getCurrentWinnerUsername());
            assertEquals("PAID", captor.getValue().getStatus());
        }

        @Test
        void finalizeExpiredSessions_WithoutWinner_BroadcastsSuccessfully() throws Exception {
            AuctionSession session = new AuctionSession();
            session.setId("SS-3");
            session.setStatus(SessionService.STATUS_FINISHED);

            when(sessionService.startDueSessions()).thenReturn(Collections.emptyList());
            when(sessionService.finalizeExpiredSessions()).thenReturn(List.of(session));

            runOnceMethod.invoke(scheduler);

            verify(userService, never()).getUserById(anyString());
            verify(dashboardRealtimeService).broadcastItemUpdatedBySessionId("SS-3", "Auction finalized");
        }

        @Test
        void exceptionInBroadcast_ContinuesLoop() throws Exception {
            AuctionSession session1 = new AuctionSession();
            session1.setId("SS-ERROR");
            AuctionSession session2 = new AuctionSession();
            session2.setId("SS-OK");

            when(sessionService.startDueSessions()).thenReturn(List.of(session1, session2));
            when(sessionService.finalizeExpiredSessions()).thenReturn(Collections.emptyList());

            doThrow(new RuntimeException("Simulated broadcast error"))
                    .when(dashboardRealtimeService).broadcastItemUpdatedBySessionId(eq("SS-ERROR"), anyString());

            runOnceMethod.invoke(scheduler);

            verify(dashboardRealtimeService).broadcastItemUpdatedBySessionId(eq("SS-OK"), anyString());
            verify(sessionWatchRegistry).broadcastBidUpdate(eq("SS-OK"), any());
        }

        @Test
        void serviceThrowsException_HandledGracefully() throws Exception {
            when(sessionService.startDueSessions()).thenThrow(new RuntimeException("DB Connection Lost"));

            assertDoesNotThrow(() -> runOnceMethod.invoke(scheduler));
            verify(sessionService, never()).finalizeExpiredSessions();
        }
    }
}