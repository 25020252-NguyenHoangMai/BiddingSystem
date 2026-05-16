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

    }