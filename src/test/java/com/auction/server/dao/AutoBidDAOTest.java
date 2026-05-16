package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.AutoBid;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AutoBidDAOTest {

    private AutoBidDAO autoBidDAO;

    @Mock private Connection mockConn;
    @Mock private PreparedStatement mockPs;
    @Mock private ResultSet mockRs;

    @BeforeEach
    void setUp() throws SQLException {
        autoBidDAO = new AutoBidDAO();
        // Cấu hình mặc định để mockConn luôn trả về mockPs khi chuẩn bị câu lệnh
        lenient().when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
    }

    @Nested
    class TestUpsertAutoBid {
        @Test
        void upsert_Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() ->
                    autoBidDAO.upsertAutoBid(mockConn, "AB1", "S1", "U1", 5000.0)
            );

            // Kiểm tra xem các tham số truyền vào PreparedStatement có đúng thứ tự logic của MERGE không
            verify(mockPs).setString(1, "S1");
            verify(mockPs).setString(2, "U1");
            verify(mockPs).setDouble(3, 5000.0);
            verify(mockPs).setString(4, "AB1");
            verify(mockPs).setDouble(5, 5000.0);
            verify(mockPs).executeUpdate();
        }

        @Test
        void upsert_ThrowsAuctionException_OnSQLException() throws SQLException {
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Database error"));

            AuctionException exception = assertThrows(AuctionException.class, () ->
                    autoBidDAO.upsertAutoBid(mockConn, "AB1", "S1", "U1", 5000.0)
            );
            assertTrue(exception.getMessage().contains("An error occurred while upserting auto-bidding"));
        }
    }

    @Nested
    class TestDeactivateAutoBid {//Deactivate: vô  hiệu hóa
        @Test
        void deactivate_Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> autoBidDAO.deactivateAutoBid(mockConn, "S1", "U1"));

            verify(mockPs).setString(1, "S1");
            verify(mockPs).setString(2, "U1");
            verify(mockPs).executeUpdate();
        }

        @Test
        void deactivate_ThrowsAuctionException_OnSQLException() throws SQLException {
            when(mockPs.executeUpdate()).thenThrow(new SQLException("DB lock error"));

            assertThrows(AuctionException.class, () ->
                    autoBidDAO.deactivateAutoBid(mockConn, "S1", "U1")
            );
        }
    }
    @Nested
    class TestGetActiveAutoBidsBySession {
        @Test
        void getActiveBids_ReturnsList_Success() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);

            when(mockRs.next()).thenReturn(true, false);


            when(mockRs.getString("id")).thenReturn("AB1");
            when(mockRs.getString("sessionId")).thenReturn("S1");
            when(mockRs.getString("bidderId")).thenReturn("U1");
            when(mockRs.getDouble("maxBidAmount")).thenReturn(3000.0);
            when(mockRs.getBoolean("isActive")).thenReturn(true);

            Timestamp now = Timestamp.valueOf(LocalDateTime.now());
            when(mockRs.getTimestamp("createdAt")).thenReturn(now);
            when(mockRs.getTimestamp("updatedAt")).thenReturn(now);

            List<AutoBid> result = autoBidDAO.getActiveAutoBidsBySession(mockConn, "S1");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("AB1", result.get(0).getId());
            assertEquals(3000.0, result.get(0).getMaxBidAmount());
            assertTrue(result.get(0).isActive());

            verify(mockPs).setString(1, "S1");
        }

        @Test
        void getActiveBids_ThrowsAuctionException_OnSQLException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("Timeout"));

            assertThrows(AuctionException.class, () ->
                    autoBidDAO.getActiveAutoBidsBySession(mockConn, "S1")
            );
        }
    }


    }