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
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

public class ItemDAOTest extends BaseDAOTest {

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


        when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
    }

    @AfterEach
    void cleanUp() {
        //pk đóng mock static sau mỗi bài test để ko ảnh hưởng bài kh
        mockedStaticDbManager.close();
    }


    @Nested
    class testInsertItem {//thêm sp

        @Test
        void Vehicle_Success() throws SQLException {
            Vehicle vehicle = new Vehicle();
            vehicle.setId("V1");
            vehicle.setName("Xe Hơi");
            vehicle.setStartingPrice(100.0);
            vehicle.setModel("Sedan");
            vehicle.setEngineType("V8");
            vehicle.setMileage(1000);

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.insertItem(vehicle));

            verify(mockPs).setString(4, "VEHICLE");
            verify(mockPs).setString(7, "Sedan");
            verify(mockPs).setNull(10, java.sql.Types.NVARCHAR);
        }

        @Test
        void Electronics_Success() throws SQLException {
            Electronics elec = new Electronics();
            elec.setId("E1");
            elec.setBrand("Samsung");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.insertItem(elec));

            verify(mockPs).setString(4, "ELECTRONICS");
            verify(mockPs).setNull(7, java.sql.Types.NVARCHAR); //model bị null
            verify(mockPs).setString(10, "Samsung");
        }

        @Test
        void Art_Success() throws SQLException {
            Art art = new Art();
            art.setId("A1");
            art.setArtist("Picasso");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.insertItem(art));

            verify(mockPs).setString(4, "ART");
            verify(mockPs).setString(11, "Picasso");
        }

        @Test
        void InvalidType_ThrowsException() {
            //tạo 1 class nặc danh kế thừa Item để test nhánh Invalid
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

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.insertItem(invalidItem));
            assertEquals("Invalid item type.", exception.getMessage());
        }

        @Test
        void SQLException_ThrowsException() throws SQLException {
            Vehicle vehicle = new Vehicle();
            when(mockPs.executeUpdate()).thenThrow(new SQLException("DB Down"));

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.insertItem(vehicle));
            assertTrue(exception.getMessage().contains("An error occurred while inserting item"));
        }
    }



    @Nested
    class testUpdateItem {

        @Test
        void Vehicle_Success() throws SQLException {
            Vehicle vehicle = new Vehicle();
            vehicle.setId("V1");
            vehicle.setModel("SUV");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.updateItem(vehicle));
            verify(mockPs).setString(3, "VEHICLE");
            verify(mockPs).setString(5, "SUV");
        }

        @Test
        void Electronics_Success() throws SQLException {
            Electronics elec = new Electronics();
            elec.setId("E1");
            elec.setBrand("Apple");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.updateItem(elec));
            verify(mockPs).setString(3, "ELECTRONICS");
            verify(mockPs).setString(8, "Apple");
        }

        @Test
        void Art_Success() throws SQLException {
            Art art = new Art();
            art.setId("A1");
            art.setArtist("Van Gogh");

            when(mockPs.executeUpdate()).thenReturn(1);

            assertDoesNotThrow(() -> itemDAO.updateItem(art));
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
            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.updateItem(invalidItem));
            assertEquals("Inavlid item type.", exception.getMessage());//mai sai chính tả trong itemdao
        }

        @Test
        void SQLException_ThrowsException() throws SQLException {
            Art art = new Art();
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Lỗi mạng"));

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.updateItem(art));
            assertTrue(exception.getMessage().contains("An error occurred while updating item information"));
        }
    }



    @Nested
    class testDeleteItem {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1); //giả lập xóa thành công 1 dòng

            assertDoesNotThrow(() -> itemDAO.deleteItem("ID-123"));
            verify(mockPs).setString(1, "ID-123");
        }

        @Test
        void NotFound_ThrowsException() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0); //ko tìm thấy dòng nào để xóa

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.deleteItem("ID-404"));
            assertEquals("The item cannot be deleted.", exception.getMessage());
        }

        @Test
        void SQLException_ThrowsException() throws SQLException {
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Lỗi xóa"));

            AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.deleteItem("ID-123"));
            assertTrue(exception.getMessage().contains("An error occurred while deleting item"));
        }
    }

    //TEST CÁC HÀM GET DỮ LIỆU BẰNG RESULTSET, BAO GỒM MAP TO DTO


    private void setupMockResultSet() throws SQLException {
        //giả lập dữ liệu cho mapToDTO đọc
        when(mockRs.getString("id")).thenReturn("ITEM-1");
        when(mockRs.getString("name")).thenReturn("Tivi Sony");
        when(mockRs.getDouble("startingPrice")).thenReturn(500.0);
    }

    @Test
    void testGetAllItems_ReturnsList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        //giả lập có 2 dòng dữ liệu
        when(mockRs.next()).thenReturn(true, true, false);
        setupMockResultSet();

        List<ItemDTO> list = itemDAO.getAllItems();
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

            ItemDTO result = itemDAO.getItemById("ITEM-1");
            assertNotNull(result);
            assertEquals("Tivi Sony", result.getName());
        }

        @Test
        void NotFound_ReturnsNull() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false); //bảng trống

            ItemDTO result = itemDAO.getItemById("GHOST-ITEM");
            assertNull(result);
        }
    }


    @Test
    void testGetItemByName_ReturnsList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, false);
        setupMockResultSet();

        List<ItemDTO> list = itemDAO.getItemByName("Tivi Sony");
        assertEquals(1, list.size());
        verify(mockPs).setString(1, "Tivi Sony");
    }

    @Test
    void testGetItemByItemType_ReturnsList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true, false);
        setupMockResultSet();

        List<ItemDTO> list = itemDAO.getItemByItemType("ELECTRONICS");
        assertEquals(1, list.size());
        verify(mockPs).setString(1, "ELECTRONICS");
    }


    @Test
    void testMapToDTO_AllFieldsMappedCorrectly() throws SQLException {
        //cbị ResultSet full dữ liệu
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(true);
        when(mockRs.getString("id")).thenReturn("FULL-1");
        when(mockRs.getString("name")).thenReturn("Tên SP");
        when(mockRs.getString("description")).thenReturn("Mô tả SP");
        when(mockRs.getString("itemType")).thenReturn("VEHICLE");
        when(mockRs.getString("sellerId")).thenReturn("USER-99");
        when(mockRs.getDouble("startingPrice")).thenReturn(999.9);
        when(mockRs.getString("model")).thenReturn("Honda");
        when(mockRs.getString("engineType")).thenReturn("V12");
        when(mockRs.getInt("mileage")).thenReturn(500);
        when(mockRs.getString("brand")).thenReturn("Toyota"); // Cố tình mix để test
        when(mockRs.getString("artist")).thenReturn("Da Vinci");

        ItemDTO result = itemDAO.getItemById("FULL-1");

        //Assert all
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


        verify(mockConn).prepareStatement(anyString());
        verify(mockPs).executeQuery();
    }


    @Test
    void testGetItemByName_EmptyResultSet_ReturnsEmptyList() throws SQLException {
        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        List<ItemDTO> list = itemDAO.getItemByName("Sản phẩm ma");

        assertTrue(list.isEmpty(), "Nếu DB không có, phải trả về List rỗng, không được null!");
        verify(mockPs).executeQuery();
    }

    @Test
    void testInsertItem_NullInput_ThrowsNullPointerException() {
        // DAO hiện tại ko handle null, sẽ bị ném NPE tại item.getId()
        // =>phải giăng bẫy bắt đc cái NPE này
        assertThrows(NullPointerException.class, () -> itemDAO.insertItem(null),
                "Phải bắt được NullPointerException khi truyền null vào DAO");
    }


    @Test
    void testGetAllItems_ThrowsSQLException() throws SQLException {

        when(mockConn.prepareStatement(anyString())).thenThrow(new SQLException("Database Connection Lost"));

        AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.getAllItems());
        assertTrue(exception.getMessage().contains("An error occurred while getting all items"));
    }

    @Test
    void testGetItemByItemType_ThrowsSQLException() throws SQLException {
        when(mockPs.executeQuery()).thenThrow(new SQLException("Table locked"));

        AuctionException exception = assertThrows(AuctionException.class, () -> itemDAO.getItemByItemType("ART"));
        assertTrue(exception.getMessage().contains("An error occurred while getting items by item type"));
    }


    @Test
    void testTryWithResources_ClosesConnectionsAutomatically() throws SQLException {

        when(mockPs.executeQuery()).thenReturn(mockRs);
        when(mockRs.next()).thenReturn(false);

        itemDAO.getItemById("TEST-CLOSE");

        //xác nhận hàm close() của ResultSet và PreparedStatement đã bị hệ thống gọi ngầm
        verify(mockRs, times(1)).close();
        verify(mockPs, times(1)).close();

    }
}