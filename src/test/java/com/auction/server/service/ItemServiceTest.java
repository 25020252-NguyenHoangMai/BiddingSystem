package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.AuctionSession;
import com.auction.model.Bidder;
import com.auction.model.Item;
import com.auction.model.User;
import com.auction.model.Vehicle;
import com.auction.server.dao.ItemDAO;
import com.auction.dto.ItemDTO;
import com.auction.server.dao.SessionDAO;
import org.junit.jupiter.api.*;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import com.auction.server.dao.DatabaseManager;
import static org.mockito.Mockito.mockStatic;


import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.Mockito.*;

public class ItemServiceTest {

    @Mock private ItemDAO itemDAO;
    @Mock private UserService userService;
    @Mock private SessionDAO sessionDAO;

    private ItemService itemService;
    private Bidder validSeller;
    private Item validItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        itemService = new ItemService(itemDAO, userService, sessionDAO);

        validSeller = new Bidder("U1", "seller_minh", "pass", "Nguyen Cong Minh", "BIDDER", 0, 0);
        validSeller.setSellerEnabled(true);

        validItem = new Vehicle();
        validItem.setId("I1");
        validItem.setName("Siêu xe UET");
        validItem.setStartingPrice(100.0);
    }


    @Nested
    class ConstructorTests {
        @Test
        void CreateSuccessfully() {
            assertDoesNotThrow(() -> new ItemService(itemDAO, userService, sessionDAO));
        }

        @Test
        void ItemDAONull() {
            assertThrows(IllegalArgumentException.class, () -> new ItemService(null, userService, sessionDAO));
        }

        @Test
        void UserServiceNull() {
            assertThrows(IllegalArgumentException.class, () -> new ItemService(itemDAO, null, sessionDAO));
        }

        @Test
        void SessionDAONull() {
            assertThrows(IllegalArgumentException.class, () -> new ItemService(itemDAO, userService, null));
        }
    }


    @Nested
    class AddItemTests {
        @Test void sellerIdNull() {
            assertThrows(AuctionException.class, () -> itemService.addItem(null, validItem));
        }

        @Test void IdBlank(){
            assertThrows(AuctionException.class, () -> itemService.addItem("   ", validItem));
        }


        @Test void userNotBidder() {
            User normalUser = new User() { // Anonymous class không phải Bidder
                @Override public String getId() { return "U1"; }
            };
            when(userService.getUserById("U1")).thenReturn(normalUser);
            assertThrows(AuctionException.class, () -> itemService.addItem("U1", validItem));
        }

        @Test void bidderNotSeller() {
            validSeller.setSellerEnabled(false);
            when(userService.getUserById("U1")).thenReturn(validSeller);
            assertThrows(AuctionException.class, () -> itemService.addItem("U1", validItem));
        }

        @Test void itemNull() {
            when(userService.getUserById("U1")).thenReturn(validSeller);
            assertThrows(AuctionException.class, () -> itemService.addItem("U1", (Item) null));
        }

        @Test void itemNameNull() {
            validItem.setName(null);
            when(userService.getUserById("U1")).thenReturn(validSeller);
            assertThrows(AuctionException.class, () -> itemService.addItem("U1", validItem));
        }

        @Test void itemNameBlank() {
            validItem.setName("   ");
            when(userService.getUserById("U1")).thenReturn(validSeller);
            assertThrows(AuctionException.class, () -> itemService.addItem("U1", validItem));
        }

        @Test void startingPriceNegative() {
            validItem.setStartingPrice(-10.0);
            when(userService.getUserById("U1")).thenReturn(validSeller);
            assertThrows(AuctionException.class, () -> itemService.addItem("U1", validItem));
        }

        @Test void success() {
            when(userService.getUserById("U1")).thenReturn(validSeller);

            itemService.addItem("U1", validItem);

            assertNotNull(validItem.getId());
            assertEquals("U1", validItem.getSellerId());
            verify(itemDAO, times(1)).insertItem(validItem);
        }
    }


    @Nested
    class AddItemDTOTests {
        @Test void itemDTONull() {
            assertThrows(AuctionException.class, () -> itemService.addItem("U1", (ItemDTO) null));
        }

        @Test void successAndChecksUppercase() {
            when(userService.getUserById("U1")).thenReturn(validSeller);
            ItemDTO dto = new ItemDTO();
            dto.setName("Laptop");
            dto.setStartingPrice(500.0);
            dto.setItemType("ELECTRONICS");
            dto.setBrand("Dell");

            ItemDTO result = itemService.addItem("U1", dto);

            assertNotNull(result.getId());
            assertEquals("U1", result.getSellerId());
            assertEquals("ELECTRONICS", result.getItemType());
            verify(itemDAO, times(1)).insertItem(any(Item.class));
        }
    }


    @Nested
    class UpdateItemTests {
        @Test void itemNull() {
            assertThrows(AuctionException.class, () -> itemService.updateItem(null));
        }

        @Test void idNull() {
            validItem.setId(null);
            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void idBlank() {
            validItem.setId("  ");
            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void nameNull() {
            validItem.setName(null);
            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void nameBlank() {
            validItem.setName("");
            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void priceNegative() {
            validItem.setStartingPrice(-1.0);
            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void itemNotFoundInDB() {
            when(itemDAO.getItemById("I1")).thenReturn(null);
            assertThrows(ItemNotFoundException.class, () -> itemService.updateItem(validItem));
        }

        @Test void sessionRunning() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            AuctionSession runningSession = new AuctionSession();
            runningSession.setStatus(SessionService.STATUS_RUNNING);
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(runningSession));

            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void sessionFinished() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            AuctionSession finishedSession = new AuctionSession();
            finishedSession.setStatus(SessionService.STATUS_FINISHED);
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(finishedSession));

            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void sessionCanceled() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            AuctionSession canceledSession = new AuctionSession();
            canceledSession.setStatus(SessionService.STATUS_CANCELED);
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(canceledSession));

            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void onlyOpenSession_Success() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            AuctionSession openSession = new AuctionSession();
            openSession.setStatus(SessionService.STATUS_OPEN);
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(openSession));

            assertDoesNotThrow(() -> itemService.updateItem(validItem));
            verify(itemDAO, times(1)).updateItem(validItem);
        }

        @Test void emptySessionsList_Success() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(Collections.emptyList());

            assertDoesNotThrow(() -> itemService.updateItem(validItem));
            verify(itemDAO, times(1)).updateItem(validItem);
        }

        @Test void multipleSessionsOneRunning() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            AuctionSession open1 = new AuctionSession(); open1.setStatus(SessionService.STATUS_OPEN);
            AuctionSession open2 = new AuctionSession(); open2.setStatus(SessionService.STATUS_OPEN);
            AuctionSession running = new AuctionSession(); running.setStatus(SessionService.STATUS_RUNNING);
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(open1, open2, running));

            assertThrows(AuctionException.class, () -> itemService.updateItem(validItem));
        }

        @Test void multipleSessionsAllOpen_Success() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            AuctionSession open1 = new AuctionSession(); open1.setStatus(SessionService.STATUS_OPEN);
            AuctionSession open2 = new AuctionSession(); open2.setStatus(SessionService.STATUS_OPEN);
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(open1, open2));

            assertDoesNotThrow(() -> itemService.updateItem(validItem));
            verify(itemDAO, times(1)).updateItem(validItem);
        }
    }


    @Nested
    class GetAllItemsTests {

        @Test void emptyList_ReturnsEmpty() {
            when(itemDAO.getAllItems()).thenReturn(Collections.emptyList());
            assertTrue(itemService.getAllItems().isEmpty());
        }

        @Test void multipleItems_ReturnsCorrectSize() {
            ItemDTO dto1 = new ItemDTO(); dto1.setItemType("VEHICLE"); dto1.setId("1");
            ItemDTO dto2 = new ItemDTO(); dto2.setItemType("ELECTRONICS"); dto2.setId("2");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto1, dto2));

            List<Item> items = itemService.getAllItems();
            assertEquals(2, items.size());
            assertEquals("1", items.get(0).getId());
        }
    }


    @Nested
    class GetAllItemDTOsTests {

        @Test
        void success_ReturnsDashboardItems() throws Exception {

            ItemDTO mockDto = new ItemDTO();
            mockDto.setId("I1");
            mockDto.setName("Siêu xe UET");
            List<ItemDTO> expectedList = List.of(mockDto);

            //giả vờ có Connection
            java.sql.Connection mockConn = mock(java.sql.Connection.class);
            DatabaseManager mockDbManager = mock(DatabaseManager.class);

            when(mockDbManager.getConnection()).thenReturn(mockConn);
            when(itemDAO.getAllItemsForDashboard(mockConn)).thenReturn(expectedList);


            try (org.mockito.MockedStatic<DatabaseManager> mockedStaticDbManager = mockStatic(DatabaseManager.class)) {
                mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);

                //gọi hàm thật
                List<ItemDTO> result = itemService.getAllItemDTOS();


                assertEquals(1, result.size());
                assertEquals("I1", result.get(0).getId());
            }
        }

        @Test
        void throwsException_OnSQLException() throws Exception {
            DatabaseManager mockDbManager = mock(DatabaseManager.class);

            when(mockDbManager.getConnection()).thenThrow(new java.sql.SQLException("DB Timeout"));

            try (org.mockito.MockedStatic<DatabaseManager> mockedStaticDbManager = mockStatic(DatabaseManager.class)) {
                mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);

                AuctionException exception = assertThrows(AuctionException.class, () -> itemService.getAllItemDTOS());
                assertTrue(exception.getMessage().contains("getting dashboard items"));
            }
        }
    }


    @Nested
    class GetItemByIdTests {
        @Test void idNull_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemById(null));
        }

        @Test void idBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemById("  "));
        }

        @Test void notFound_ThrowsException() {
            when(itemDAO.getItemById("I1")).thenReturn(null);
            assertThrows(ItemNotFoundException.class, () -> itemService.getItemById("I1"));
        }

        @Test void success_ReturnsItem() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1"); dto.setItemType("VEHICLE");
            when(itemDAO.getItemById("I1")).thenReturn(dto);

            Item item = itemService.getItemById("I1");
            assertNotNull(item);
            assertEquals("I1", item.getId());
        }
    }


    @Nested
    class GetItemByNameTests {
        @Test void nameNullOrBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemByName(null));
            assertThrows(AuctionException.class, () -> itemService.getItemByName("   "));
        }

        @Test void emptyList_ThrowsException() {
            when(itemDAO.getItemByName("Laptop")).thenReturn(Collections.emptyList());
            assertThrows(ItemNotFoundException.class, () -> itemService.getItemByName("Laptop"));
        }

        @Test void success_ReturnsMappedItems() {
            ItemDTO dto = new ItemDTO(); dto.setItemType("ELECTRONICS");
            when(itemDAO.getItemByName("Laptop")).thenReturn(List.of(dto, dto));

            List<Item> items = itemService.getItemByName("Laptop");
            assertEquals(2, items.size());
        }
    }


    @Nested
    class GetItemByTypeTests {
        @Test void typeNullOrBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemByItemType(null));
            assertThrows(AuctionException.class, () -> itemService.getItemByItemType("   "));
        }

        @Test void emptyList_ThrowsException() {
            when(itemDAO.getItemByItemType("ART")).thenReturn(Collections.emptyList());
            assertThrows(ItemNotFoundException.class, () -> itemService.getItemByItemType("ART"));
        }

        @Test void success_ReturnsMappedItems() {
            ItemDTO dto = new ItemDTO(); dto.setItemType("ART");
            when(itemDAO.getItemByItemType("ART")).thenReturn(List.of(dto));

            List<Item> items = itemService.getItemByItemType("ART");
            assertEquals(1, items.size());
        }
    }


    @Nested
    class RemoveItemTests {
        @Test void idNullOrBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.removeItem(null));
            assertThrows(AuctionException.class, () -> itemService.removeItem("  "));
        }

        @Test void notFound_ThrowsException() {
            when(itemDAO.getItemById("I1")).thenReturn(null);
            assertThrows(ItemNotFoundException.class, () -> itemService.removeItem("I1"));
        }

        @Test void hasSession_ThrowsException() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            when(sessionDAO.existsSessionByItemId("I1")).thenReturn(true);

            AuctionException e = assertThrows(AuctionException.class, () -> itemService.removeItem("I1"));
            assertTrue(e.getMessage().contains("already been used in an auction session"));
        }

        @Test void success_CallsDelete() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            when(sessionDAO.existsSessionByItemId("I1")).thenReturn(false);

            assertDoesNotThrow(() -> itemService.removeItem("I1"));
            verify(itemDAO, times(1)).deleteItem("I1");
        }
    }
}