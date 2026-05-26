package com.auction.server.dao;

import com.auction.dto.SessionHistoryItemDTO;
import com.auction.exception.AuctionException;
import com.auction.model.BidTransaction;
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
class BidDAOTest {

    @Mock private Connection mockConn;
    @Mock private PreparedStatement mockPs;
    @Mock private ResultSet mockRs;

    private BidDAO bidDAO;

    @BeforeEach
    void setUp() throws SQLException {
        bidDAO = new BidDAO();
        lenient().when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
    }

    @Nested
    class InsertBidTests {
        @Test
        void insertBid_Success() throws SQLException {
            BidTransaction bid = new BidTransaction("bid1", "ss1", "user1", 1500.0);
            bid.setBidTime(LocalDateTime.now());

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> bidDAO.insertBid(mockConn, bid));

            verify(mockPs).setString(1, "bid1");
            verify(mockPs).setString(2, "ss1");
            verify(mockPs).setDouble(4, 1500.0);
            verify(mockPs).executeUpdate();
        }

        @Test
        void insertBid_ThrowsSQLException() throws SQLException {
            BidTransaction bid = new BidTransaction("bid1", "ss1", "user1", 1500.0);
            bid.setBidTime(LocalDateTime.now());

            when(mockPs.executeUpdate()).thenThrow(new SQLException("DB Insert Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.insertBid(mockConn, bid));
            assertTrue(exception.getMessage().contains("An error occurred while placing bid"));
        }
    }

    @Nested
    class GetHighestBidTests {
        @Test
        void getHighestBid_Found_ReturnsBid() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);

            lenient().when(mockRs.getString("id")).thenReturn("bid1");
            lenient().when(mockRs.getString("sessionId")).thenReturn("ss1");
            lenient().when(mockRs.getString("bidderId")).thenReturn("user1");
            lenient().when(mockRs.getDouble("bidAmount")).thenReturn(2000.0);
            lenient().when(mockRs.getTimestamp("bidTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

            BidTransaction result = bidDAO.getHighestBid(mockConn, "ss1");

            assertNotNull(result);
            assertEquals("bid1", result.getId());
            assertEquals(2000.0, result.getBidAmount());
        }

        @Test
        void getHighestBid_NotFound_ReturnsNull() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);

            BidTransaction result = bidDAO.getHighestBid(mockConn, "ss1");

            assertNull(result);
        }

        @Test
        void getHighestBid_TimestampNull_ReturnsBidWithNullTime() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);

            lenient().when(mockRs.getString("id")).thenReturn("bid1");
            lenient().when(mockRs.getTimestamp("bidTime")).thenReturn(null);

            BidTransaction result = bidDAO.getHighestBid(mockConn, "ss1");

            assertNotNull(result);
            assertNull(result.getBidTime());
        }

        @Test
        void getHighestBid_ThrowsSQLException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("DB Query Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getHighestBid(mockConn, "ss1"));
            assertTrue(exception.getMessage().contains("An error occurred while getting highest bid"));
        }
    }

    @Nested
    class GetBidsBySessionTests {
        @Test
        void getBidsBySession_FoundMultiple_ReturnsList() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true, true, false);

            lenient().when(mockRs.getString("id")).thenReturn("bid1", "bid2");
            lenient().when(mockRs.getString("sessionId")).thenReturn("ss1", "ss1");
            lenient().when(mockRs.getTimestamp("bidTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

            List<BidTransaction> result = bidDAO.getBidsBySession(mockConn, "ss1");

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        void getBidsBySession_ThrowsSQLException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("DB Query Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getBidsBySession(mockConn, "ss1"));
            assertTrue(exception.getMessage().contains("An error occurred while getting bids by session"));
        }
    }

    @Nested
    class GetBidsByBidderTests {
        @Test
        void getBidsByBidder_Found_ReturnsList() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true, false);

            lenient().when(mockRs.getString("id")).thenReturn("bid1");
            lenient().when(mockRs.getString("sessionId")).thenReturn("ss1");
            lenient().when(mockRs.getString("bidderId")).thenReturn("user1");
            lenient().when(mockRs.getTimestamp("bidTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

            List<BidTransaction> result = bidDAO.getBidsByBidder(mockConn, "user1");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(mockPs).setString(1, "user1");
        }

        @Test
        void getBidsByBidder_ThrowsSQLException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("DB Query Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getBidsByBidder(mockConn, "user1"));
            assertTrue(exception.getMessage().contains("An error occurred while getting bids by bidder"));
        }
    }

    @Nested
    class GetSessionHistoryByBidderTests {
        @Test
        void getSessionHistory_Found_ReturnsList() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true, false);

            lenient().when(mockRs.getString("sessionId")).thenReturn("ss1");
            lenient().when(mockRs.getString("productName")).thenReturn("Laptop Dell");
            lenient().when(mockRs.getDouble("userLastBid")).thenReturn(1500.0);
            lenient().when(mockRs.getDouble("currentPrice")).thenReturn(2000.0);
            lenient().when(mockRs.getString("status")).thenReturn("WON");

            List<SessionHistoryItemDTO> result = bidDAO.getSessionHistoryByBidder(mockConn, "user1");

            assertNotNull(result);
            assertEquals(1, result.size());
            assertEquals("Laptop Dell", result.get(0).getProductName());
            assertEquals("WON", result.get(0).getStatus());

            verify(mockPs).setString(1, "user1");
            verify(mockPs).setString(2, "user1");
        }

        @Test
        void getSessionHistory_ThrowsSQLException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("DB Query Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getSessionHistoryByBidder(mockConn, "user1"));
            assertTrue(exception.getMessage().contains("An error occurred while getting session history"));
        }
    }

    @Nested
    class ExistsBidBySessionIdTests {
        @Test
        void existsBid_ReturnsTrue() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);

            boolean result = bidDAO.existsBidBySessionId(mockConn, "ss1");

            assertTrue(result);
            verify(mockPs).setString(1, "ss1");
        }

        @Test
        void notExistsBid_ReturnsFalse() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);

            boolean result = bidDAO.existsBidBySessionId(mockConn, "ss1");

            assertFalse(result);
            verify(mockPs).setString(1, "ss1");
        }

        @Test
        void throwsSQLException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("DB Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.existsBidBySessionId(mockConn, "ss1"));
            assertTrue(exception.getMessage().contains("checking session bids"));
        }
    }

    @Nested
    class GetVisibleSessionIdsByBidderIdTests {
        @Test
        void foundSessions_ReturnsList() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true, true, false);

            lenient().when(mockRs.getString("sessionId")).thenReturn("ss1", "ss2");

            List<String> result = bidDAO.getVisibleSessionIdsByBidderId(mockConn, "bidder1");

            assertNotNull(result);
            assertEquals(2, result.size());
            assertTrue(result.contains("ss1"));
            assertTrue(result.contains("ss2"));

            verify(mockPs).setString(1, "bidder1");
        }

        @Test
        void throwsSQLException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("DB Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getVisibleSessionIdsByBidderId(mockConn, "bidder1"));
            assertTrue(exception.getMessage().contains("getting bidder sessions"));
        }
    }
}