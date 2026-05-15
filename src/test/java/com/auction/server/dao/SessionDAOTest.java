package com.auction.server.dao;

import com.auction.dto.ItemDTO;
import com.auction.exception.AuctionException;
import com.auction.model.AuctionSession;
import com.auction.model.Item;
import com.auction.server.factory.ItemFromDTOFactory;
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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)

@MockitoSettings(strictness = Strictness.LENIENT)
class SessionDAOTest {

    @Mock private ItemDAO itemDAO;
    @Mock private Connection connection;
    @Mock private PreparedStatement preparedStatement;
    @Mock private ResultSet resultSet;
    @Mock private DatabaseManager databaseManager;

    private SessionDAO sessionDAO;

    private MockedStatic<DatabaseManager> mockedDatabaseManager;
    private MockedStatic<ItemFromDTOFactory> mockedFactory;

    @BeforeEach
    void setUp() throws SQLException {
        sessionDAO = new SessionDAO(itemDAO);

        mockedDatabaseManager = mockStatic(DatabaseManager.class);
        mockedDatabaseManager.when(DatabaseManager::getInstance).thenReturn(databaseManager);
        when(databaseManager.getConnection()).thenReturn(connection);

        mockedFactory = mockStatic(ItemFromDTOFactory.class);
    }

    @AfterEach
    void cleanUp() {
        mockedDatabaseManager.close();
        mockedFactory.close();
    }

    @Test
    void testConstructor_NullItemDAO_ThrowsException() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class, () -> {
            new SessionDAO(null);
        });
        assertEquals("ItemDAO must not be null", exception.getMessage());
    }


    @Nested
    class testInsertSession {
        @Test
        void Success() throws SQLException {
            AuctionSession session = new AuctionSession("ss1", mock(Item.class), LocalDateTime.now(), LocalDateTime.now().plusDays(1));
            session.setCurrentPrice(500.0);
            session.setCurrentWinnerId("user1");
            session.setStatus("OPEN");
            Item item = mock(Item.class);
            when(item.getId()).thenReturn("item1");

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> sessionDAO.insertSession(session, item));

            verify(preparedStatement, times(1)).executeUpdate();
        }

        @Test
        void ThrowsSQLException() throws SQLException {
            AuctionSession session = new AuctionSession("ss1", mock(Item.class), LocalDateTime.now(), LocalDateTime.now().plusDays(1));
            Item item = mock(Item.class);

            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeUpdate()).thenThrow(new SQLException("DB Error"));

            AuctionException exception = assertThrows(AuctionException.class, () -> sessionDAO.insertSession(session, item));
            assertTrue(exception.getMessage().contains("An error occurred while inserting session"));
        }
    }


    @Nested
    class testGetSessionById {
        @Test
        void Found_Success() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            when(resultSet.getString("id")).thenReturn("ss1");
            when(resultSet.getString("itemId")).thenReturn("item1");
            when(resultSet.getDouble("currentPrice")).thenReturn(1000.0);
            when(resultSet.getString("currentWinnerId")).thenReturn("user2");
            when(resultSet.getTimestamp("startTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now()));
            when(resultSet.getTimestamp("endTime")).thenReturn(Timestamp.valueOf(LocalDateTime.now().plusDays(2)));
            when(resultSet.getString("status")).thenReturn("RUNNING");

            ItemDTO mockDto = new ItemDTO();
            when(itemDAO.getItemById("item1")).thenReturn(mockDto);

            Item mockItem = mock(Item.class);
            mockedFactory.when(() -> ItemFromDTOFactory.createItem(mockDto)).thenReturn(mockItem);

            AuctionSession result = sessionDAO.getSessionById("ss1");

            assertNotNull(result);
            assertEquals("ss1", result.getId());
            assertEquals(1000.0, result.getCurrentPrice());
            assertEquals("RUNNING", result.getStatus());
        }

        @Test
        void ItemNotFound_ThrowsException() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);
            when(resultSet.getString("itemId")).thenReturn("item1");

            when(itemDAO.getItemById("item1")).thenReturn(null);

            AuctionException exception = assertThrows(AuctionException.class, () -> sessionDAO.getSessionById("ss1"));
            assertTrue(exception.getMessage().contains("Item not found"));
        }
    }


    @Test
    void testUpdateCurrentBid_Success() throws SQLException {

        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);

        when(preparedStatement.executeUpdate()).thenReturn(1);

        boolean result = sessionDAO.updateCurrentBid(connection, "ss1", 1500.0, "user3");

        assertTrue(result);
        verify(preparedStatement).setDouble(1, 1500.0);
        verify(preparedStatement).setString(3, "ss1");
    }

    @Test
    void testUpdateStatus_NotFound() throws SQLException {
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeUpdate()).thenReturn(0);

        AuctionException exception = assertThrows(AuctionException.class, () -> sessionDAO.updateStatus(connection, "ss1", "CLOSED"));
        assertEquals("Auction session is not found to update status.", exception.getMessage());
    }


    @Nested
    class testExistsActiveSessionByItemId {
        @Test
        void True() throws SQLException {
            when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
            when(preparedStatement.executeQuery()).thenReturn(resultSet);
            when(resultSet.next()).thenReturn(true);

            boolean exists = sessionDAO.existsActiveSessionByItemId("item1");

            assertTrue(exists);
        }

        @Test
        void NullParam_ThrowsException() {
            assertThrows(IllegalArgumentException.class, () -> sessionDAO.existsActiveSessionByItemId(null));
        }
    }
}