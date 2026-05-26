package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.Art;
import com.auction.model.Electronics;
import com.auction.model.Item;
import com.auction.model.Vehicle;
import com.auction.dto.ItemDTO;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ItemDAOTest  {

    private ItemDAO itemDAO;

    @Mock
    private Connection mockConn;
    @Mock
    private PreparedStatement mockPs;
    @Mock
    private ResultSet mockRs;
    @Mock
    private DatabaseManager mockDbManager;

    private MockedStatic<DatabaseManager> mockedStaticDbManager;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);
        itemDAO = new ItemDAO();

        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);

        lenient().when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
    }

    @AfterEach
    void cleanUp() {
        mockedStaticDbManager.close();
    }

    @Nested
    class testInsertItem {

        @Test
        void Vehicle_Success() throws SQLException {
            Vehicle vehicle = new Vehicle();
            vehicle.setId("V1");
            vehicle.setName("Xe Hơi");
            vehicle.setDescription("Xe 4 chỗ");
            vehicle.setItemType("VEHICLE");
            vehicle.setSellerId("USER-1");
            vehicle.setStartingPrice(100.0);
            vehicle.setModel("Sedan");
            vehicle.setEngineType("V8");
            vehicle.setMileage(1000);

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.insertItem(mockConn, vehicle));

            verify(mockPs).setString(4, "VEHICLE");
            verify(mockPs).setString(7, "Sedan");
            verify(mockPs).setNull(10, java.sql.Types.NVARCHAR);
        }

        @Test
        void Electronics_Success() throws SQLException {
            Electronics elec = new Electronics();
            elec.setId("E1");
            elec.setName("Điện thoại");
            elec.setDescription("Mới 100%");
            elec.setItemType("ELECTRONICS");
            elec.setSellerId("USER-2");
            elec.setBrand("Samsung");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.insertItem(mockConn, elec));

            verify(mockPs).setString(4, "ELECTRONICS");
            verify(mockPs).setNull(7, java.sql.Types.NVARCHAR);
            verify(mockPs).setString(10, "Samsung");
        }

        @Test
        void Art_Success() throws SQLException {
            Art art = new Art();
            art.setId("A1");
            art.setName("Tranh chân dung");
            art.setDescription("Sơn dầu");
            art.setItemType("ART");
            art.setSellerId("USER-3");
            art.setArtist("Picasso");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.insertItem(mockConn, art));

            verify(mockPs).setString(4, "ART");
            verify(mockPs).setString(11, "Picasso");
        }

        @Test
        void InvalidType_ThrowsException() {
            Item invalidItem = new Item() {
                @Override
                public String getId() {
                    return "I1";
                }

                @Override
                public String getCategoryDetails() {
                    return "Vật phẩm Fake để test Insert";
                }
            };

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.insertItem(mockConn, invalidItem));
            assertEquals("Invalid item type.", exception.getMessage());
        }

        @Test
        void SQLException_ThrowsException() throws SQLException {
            Vehicle vehicle = new Vehicle();
            when(mockPs.executeUpdate()).thenThrow(new SQLException("DB Down"));

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.insertItem(mockConn, vehicle));
            assertTrue(exception.getMessage().contains("An error occurred while inserting item"));
        }
    }

    @Nested
    class testUpdateItem {

        @Test
        void Vehicle_Success() throws SQLException {
            Vehicle vehicle = new Vehicle();
            vehicle.setId("V1");
            vehicle.setName("Xe SUV");
            vehicle.setDescription("Xe 7 chỗ");
            vehicle.setItemType("VEHICLE");
            vehicle.setStartingPrice(100.0);
            vehicle.setModel("SUV");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.updateItem(mockConn, vehicle));
            verify(mockPs).setString(3, "VEHICLE");
            verify(mockPs).setString(5, "SUV");
        }

        @Test
        void Electronics_Success() throws SQLException {
            Electronics elec = new Electronics();
            elec.setId("E1");
            elec.setName("Đồng hồ");
            elec.setDescription("Thông minh");
            elec.setItemType("ELECTRONICS");
            elec.setStartingPrice(50.0);
            elec.setBrand("Apple");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.updateItem(mockConn, elec));
            verify(mockPs).setString(3, "ELECTRONICS");
            verify(mockPs).setString(8, "Apple");
        }

        @Test
        void Art_Success() throws SQLException {
            Art art = new Art();
            art.setId("A1");
            art.setName("Tranh vẽ");
            art.setDescription("Đẹp");
            art.setItemType("ART");
            art.setStartingPrice(200.0);
            art.setArtist("Van Gogh");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.updateItem(mockConn, art));
            verify(mockPs).setString(3, "ART");
            verify(mockPs).setString(9, "Van Gogh");
        }

        @Test
        void InvalidType_ThrowsException() {
            Item invalidItem = new Item() {
                @Override
                public String getCategoryDetails() {
                    return "Vật phẩm Fake để test";
                }
            };
            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.updateItem(mockConn, invalidItem));
            assertEquals("Inavlid item type.", exception.getMessage());
        }

        @Test
        void SQLException_ThrowsException() throws SQLException {
            Art art = new Art();
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Lỗi mạng"));

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.updateItem(mockConn, art));
            assertTrue(exception.getMessage().contains("An error occurred while updating item information"));
        }
    }

    @Nested
    class testDeleteItem {
        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> itemDAO.deleteItem(mockConn, "ID-123"));
            verify(mockPs).setString(1, "ID-123");
        }

        @Test
        void NotFound_ThrowsException() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0);
            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.deleteItem(mockConn, "ID-404"));
            assertEquals("The item cannot be deleted.", exception.getMessage());
        }

        @Test
        void SQLException_ThrowsException() throws SQLException {
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Lỗi xóa"));
            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.deleteItem(mockConn, "ID-123"));
            assertTrue(exception.getMessage().contains("An error occurred while deleting item"));
        }
    }

    private void setupMockResultSet() throws SQLException {
        lenient().when(mockRs.getString("id")).thenReturn("ITEM-1");
        lenient().when(mockRs.getString("name")).thenReturn("Tivi Sony");
        lenient().when(mockRs.getDouble("startingPrice")).thenReturn(500.0);
    }

    @Test
    void testGetAllItems_ReturnsList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, true, false);
        setupMockResultSet();

        List<ItemDTO> list = itemDAO.getAllItems(mockConn);
        assertEquals(2, list.size());
        assertEquals("ITEM-1", list.get(0).getId());
    }

    @Nested
    class testGetItemById {
        @Test
        void Found_ReturnsDTO() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            setupMockResultSet();

            ItemDTO result = itemDAO.getItemById(mockConn, "ITEM-1");
            assertNotNull(result);
            assertEquals("Tivi Sony", result.getName());
        }

        @Test
        void NotFound_ReturnsNull() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);

            ItemDTO result = itemDAO.getItemById(mockConn, "GHOST-ITEM");
            assertNull(result);
        }
    }

    @Test
    void testGetItemByName_ReturnsList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, false);
        setupMockResultSet();

        List<ItemDTO> list = itemDAO.getItemByName(mockConn, "Tivi Sony");
        assertEquals(1, list.size());
        verify(mockPs).setString(1, "Tivi Sony");
    }

    @Test
    void testGetItemByItemType_ReturnsList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, false);
        setupMockResultSet();

        List<ItemDTO> list = itemDAO.getItemByItemType(mockConn, "ELECTRONICS");
        assertEquals(1, list.size());
        verify(mockPs).setString(1, "ELECTRONICS");
    }

    @Test
    void testMapToDTO_AllFieldsMappedCorrectly() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        lenient().when(mockRs.getString("id")).thenReturn("FULL-1");
        lenient().when(mockRs.getString("name")).thenReturn("Tên SP");
        lenient().when(mockRs.getString("description")).thenReturn("Mô tả SP");
        lenient().when(mockRs.getString("itemType")).thenReturn("VEHICLE");
        lenient().when(mockRs.getString("sellerId")).thenReturn("USER-99");
        lenient().when(mockRs.getDouble("startingPrice")).thenReturn(999.9);
        lenient().when(mockRs.getString("model")).thenReturn("Honda");
        lenient().when(mockRs.getString("engineType")).thenReturn("V12");
        lenient().when(mockRs.getInt("mileage")).thenReturn(500);
        lenient().when(mockRs.getString("brand")).thenReturn("Toyota");
        lenient().when(mockRs.getString("artist")).thenReturn("Da Vinci");

        ItemDTO result = itemDAO.getItemById(mockConn, "FULL-1");

        assertNotNull(result);
        assertEquals("FULL-1", result.getId());
        assertEquals("Tên SP", result.getName());
        assertEquals("Mô tả SP", result.getDescription());
        assertEquals("VEHICLE", result.getItemType());
        assertEquals("USER-99", result.getSellerId());
        assertEquals(999.9, result.getStartingPrice());
        assertEquals("Honda", result.getModel());
        assertEquals("V12", result.getEngineType());
        assertEquals(500, result.getMileage());
        assertEquals("Toyota", result.getBrand());
        assertEquals("Da Vinci", result.getArtist());
    }

    @Test
    void testGetItemByName_EmptyResultSet_ReturnsEmptyList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        List<ItemDTO> list = itemDAO.getItemByName(mockConn, "Sản phẩm ma");
        assertTrue(list.isEmpty());
    }

    @Test
    void testInsertItem_NullInput_ThrowsNullPointerException() {
        assertThrows(NullPointerException.class, () -> itemDAO.insertItem(mockConn, null));
    }

    @Test
    void testGetAllItems_ThrowsSQLException() throws SQLException {
        when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("Database Connection Lost"));
        AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.getAllItems(mockConn));
        assertTrue(exception.getMessage().contains("An error occurred while getting all items"));
    }

    @Test
    void testGetItemByItemType_ThrowsSQLException() throws SQLException {
        when(mockPs.executeQuery()).thenThrow(new SQLException("Table locked"));
        AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.getItemByItemType(mockConn, "ART"));
        assertTrue(exception.getMessage().contains("An error occurred while getting items by item type"));
    }

    @Test
    void testTryWithResources_ClosesConnectionsAutomatically() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        itemDAO.getItemById(mockConn, "TEST-CLOSE");

        verify(mockRs, times(1)).close();
        verify(mockPs, times(1)).close();
    }

    @Nested
    class DashboardTests {

        @Test
        void testGetAllItemsForDashboard_ReturnsList() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true, false);
            setupMockResultSet();

            lenient().when(mockRs.getString("sellerUsername")).thenReturn("seller_user");
            lenient().when(mockRs.getString("sessionId")).thenReturn("SS-1");
            lenient().when(mockRs.getDouble("currentPrice")).thenReturn(600.0);
            lenient().when(mockRs.getString("sessionStatus")).thenReturn("RUNNING");
            lenient().when(mockRs.getString("currentWinnerUsername")).thenReturn("winner_user");
            lenient().when(mockRs.getTimestamp("startTime")).thenReturn(new Timestamp(System.currentTimeMillis()));
            lenient().when(mockRs.getTimestamp("endTime")).thenReturn(new Timestamp(System.currentTimeMillis()));

            List<ItemDTO> list = itemDAO.getAllItemsForDashboard(mockConn);
            assertEquals(1, list.size());
            assertEquals("seller_user", list.get(0).getSellerUsername());
            assertEquals("RUNNING", list.get(0).getSessionStatus());
        }

        @Test
        void testGetDashboardItemsBySellerId_ReturnsList() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true, false);
            setupMockResultSet();

            lenient().when(mockRs.getString("sellerUsername")).thenReturn("seller_user");

            List<ItemDTO> list = itemDAO.getDashboardItemsBySellerId(mockConn, "SELLER-1");
            assertEquals(1, list.size());
            verify(mockPs).setString(1, "SELLER-1");
        }

        @Test
        void dashboard_ThrowsSQLException() throws SQLException {
            when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("DB Dashboard Error"));
            assertThrows(AuctionException.class, () -> itemDAO.getAllItemsForDashboard(mockConn));
            assertThrows(AuctionException.class, () -> itemDAO.getDashboardItemsBySellerId(mockConn, "U1"));
        }
    }
}