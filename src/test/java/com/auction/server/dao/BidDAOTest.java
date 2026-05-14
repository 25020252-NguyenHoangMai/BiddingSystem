package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.BidTransaction;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class BidDAOTest {

    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;
    @Mock private DatabaseManager databaseManager;

    private BidDAO bidDAO;
    private MockedStatic<DatabaseManager> mockedDatabaseManager;

    @BeforeEach
    void setUp() throws SQLException {
        bidDAO = new BidDAO();


        mockedDatabaseManager = mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getInstance).thenReturn(databaseManager);
        when(databaseManager.getConnection()).thenReturn(connection);


        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
    }

    @AfterEach
    void tearDown() {

        mockedDatabaseManager.close();
    }

    @Nested
    class InsertBidTests {
        @Test
        void insertBid_Success() throws SQLException {
            BidTransaction bid = new BidTransaction("bid1", "ss1", "user1", 1500.0);
            bid.setBidTime(LocalDateTime.now());

            when(preparedStatement.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> bidDAO.insertBid(connection, bid));

            verify(preparedStatement).setString(1, "bid1");
            verify(preparedStatement).setString(2, "ss1");
            verify(preparedStatement).setDouble(4, 1500.0);
            verify(preparedStatement).executeUpdate();
        }

        @Test
        void insertBid_ThrowsSQLException() throws SQLException {
            BidTransaction bid = new BidTransaction("bid1", "ss1", "user1", 1500.0);
            bid.setBidTime(LocalDateTime.now());

            when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB Insert Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.insertBid(connection, bid));
            assertTrue(exception.getMessage().contains("An error occurred while placing bid"));
        }
    }

    @Nested
    class GetHighestBidTests {
        @Test
        void getHighestBid_Found_ReturnsBid() throws SQLException {
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            when(resultSet.getString("id")).thenReturn("bid1");
            when(resultSet.getString("sessionId")).thenReturn("ss1");
            when(resultSet.getString("bidderId")).thenReturn("user1");
            when(resultSet.getDouble("bidAmount")).thenReturn(2000.0);
            when(resultSet.getTimestamp("bidTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

            BidTransaction result = bidDAO.getHighestBid("ss1");

            assertNotNull(result);
            assertEquals("bid1", result.getId());
            assertEquals(2000.0, result.getBidAmount());
        }

        @Test
        void getHighestBid_NotFound_ReturnsNull() throws SQLException {
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(false); // Không có dữ liệu

            BidTransaction result = bidDAO.getHighestBid("ss1");

            assertNull(result);
        }

        @Test
        void getHighestBid_TimestampNull_ReturnsBidWithNullTime() throws SQLException {

            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            when(resultSet.getString("id")).thenReturn("bid1");
            when(resultSet.getTimestamp("bidTime")).thenReturn(null);

            BidTransaction result = bidDAO.getHighestBid("ss1");

            assertNotNull(result);
            assertNull(result.getBidTime());
        }

        @Test
        void getHighestBid_ThrowsSQLException() throws SQLException {
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB Query Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getHighestBid("ss1"));
            assertTrue(exception.getMessage().contains("An error occurred while getting highest bid"));
        }
    }

    @Nested
    class GetBidsBySessionTests {
        @Test
        void getBidsBySession_FoundMultiple_ReturnsList() throws SQLException {
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            // Giả lập trả về 2 dòng dữ liệu
            when(resultSet.next()).thenReturn(true, true, false);

            when(resultSet.getString("id")).thenReturn("bid1", "bid2");
            when(resultSet.getString("sessionId")).thenReturn("ss1");
            when(resultSet.getTimestamp("bidTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

            List<BidTransaction> result = bidDAO.getBidsBySession("ss1");

            assertNotNull(result);
            assertEquals(2, result.size());
        }

        @Test
        void getBidsBySession_ThrowsSQLException() throws SQLException {
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB Query Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getBidsBySession("ss1"));
            assertTrue(exception.getMessage().contains("An error occurred while getting bids by session"));
        }
    }

    @Nested
    class GetBidsByBidderTests {
        @Test
        void getBidsByBidder_Found_ReturnsList() throws SQLException {
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true, false);

            when(resultSet.getString("id")).thenReturn("bid1");
            when(resultSet.getString("sessionId")).thenReturn("ss1");
            when(resultSet.getString("bidderId")).thenReturn("user1");
            when(resultSet.getTimestamp("bidTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));

            List<BidTransaction> result = bidDAO.getBidsByBidder("ss1", "user1");

            assertNotNull(result);
            assertEquals(1, result.size());
            verify(preparedStatement).setString(1, "ss1");
            verify(preparedStatement).setString(2, "user1");
        }

        @Test
        void getBidsByBidder_ThrowsSQLException() throws SQLException {
            when(preparedStatement.executeQuery()).thenThrow(new SQLException("DB Query Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> bidDAO.getBidsByBidder("ss1", "user1"));
            assertTrue(exception.getMessage().contains("An error occurred while getting bids by bidder"));
        }
    }
}