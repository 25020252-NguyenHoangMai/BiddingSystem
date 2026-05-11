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

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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


    //CONSTRUCTOR TESTS

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


    //ADD ITEM

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


    //ADD ITEM DTO

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


    //UPDATE ITEM

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


    //GET ALL ITEMS

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


    //GET ALL ITEM DTOs

    @Nested
    class GetAllItemDTOsTests {
        @Test void emptyList_ReturnsEmpty() {
            when(itemDAO.getAllItems()).thenReturn(Collections.emptyList());
            assertTrue(itemService.getAllItemDTOS().isEmpty());
        }

        @Test void sellerIdNull_DoesNotCallUserService() {
            ItemDTO dto = new ItemDTO(); dto.setSellerId(null);
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));

            itemService.getAllItemDTOS();
            verify(userService, never()).getUserById(anyString());
        }

        @Test void sellerIdBlank_DoesNotCallUserService() {
            ItemDTO dto = new ItemDTO(); dto.setSellerId("  ");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));

            itemService.getAllItemDTOS();
            verify(userService, never()).getUserById(anyString());
        }

        @Test void lookupSellerSuccess_SetsUsername() {
            ItemDTO dto = new ItemDTO(); dto.setSellerId("U1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));
            when(userService.getUserById("U1")).thenReturn(validSeller); // validSeller có user "seller_minh"

            List<ItemDTO> result = itemService.getAllItemDTOS();
            assertEquals("seller_minh", result.get(0).getSellerUsername());
        }

        @Test void lookupSellerThrowsException_IgnoresAndContinues() {
            ItemDTO dto = new ItemDTO(); dto.setSellerId("U1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));
            when(userService.getUserById("U1")).thenThrow(new RuntimeException("DB Error"));

            assertDoesNotThrow(() -> {
                List<ItemDTO> result = itemService.getAllItemDTOS();
                assertNull(result.get(0).getSellerUsername());
            });
        }

        @Test void noSession_DoesNotSetSessionFields() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(Collections.emptyList());

            List<ItemDTO> result = itemService.getAllItemDTOS();
            assertNull(result.get(0).getSessionId());
        }

        @Test void hasSession_SetsSessionFieldsAndEndTime() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));

            AuctionSession session = new AuctionSession();
            session.setId("S1");
            session.setCurrentPrice(500.0);
            session.setStatus(SessionService.STATUS_RUNNING);
            session.setEndTime(LocalDateTime.now());
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(session));

            List<ItemDTO> result = itemService.getAllItemDTOS();
            ItemDTO resDto = result.get(0);
            assertEquals("S1", resDto.getSessionId());
            assertEquals(500.0, resDto.getCurrentPrice());
            assertEquals(SessionService.STATUS_RUNNING, resDto.getSessionStatus());
            assertNotNull(resDto.getEndTimeMillis());
        }

        @Test void hasSession_EndTimeNull_DoesNotSetMillis() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));
            AuctionSession session = new AuctionSession();
            session.setEndTime(null); // Không có giờ kết thúc
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(session));

            List<ItemDTO> result = itemService.getAllItemDTOS();

            assertEquals(0L, result.get(0).getEndTimeMillis(), "Khi endTime null, giá trị millis mặc định phải là 0");
        }

        @Test void winnerIdNullOrBlank_DoesNotCallUserService() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));
            AuctionSession session = new AuctionSession();
            session.setCurrentWinnerId(" ");
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(session));

            itemService.getAllItemDTOS();
            verify(userService, never()).getUserById(anyString());
        }

        @Test void lookupWinnerSuccess_SetsWinnerUsername() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));
            AuctionSession session = new AuctionSession();
            session.setCurrentWinnerId("W1");
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(session));

            User winner = new Bidder("W1", "winner_bro", "pass", "Name", "BIDDER", 0, 0);
            when(userService.getUserById("W1")).thenReturn(winner);

            List<ItemDTO> result = itemService.getAllItemDTOS();
            assertEquals("winner_bro", result.get(0).getCurrentWinnerUsername());
        }

        @Test void lookupWinnerThrowsException_IgnoresAndContinues() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1");
            when(itemDAO.getAllItems()).thenReturn(List.of(dto));
            AuctionSession session = new AuctionSession();
            session.setCurrentWinnerId("W1");
            when(sessionDAO.getSessionsByItemId("I1")).thenReturn(List.of(session));
            when(userService.getUserById("W1")).thenThrow(new RuntimeException("Network error"));

            assertDoesNotThrow(() -> {
                List<ItemDTO> result = itemService.getAllItemDTOS();
                assertNull(result.get(0).getCurrentWinnerUsername());
            });
        }
    }


    //GET ITEM BY ID

    @Nested
    class GetItemByIdTests {
        @Test void getItemById_idNull_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemById(null));
        }

        @Test void getItemById_idBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemById("  "));
        }

        @Test void getItemById_notFound_ThrowsException() {
            when(itemDAO.getItemById("I1")).thenReturn(null);
            assertThrows(ItemNotFoundException.class, () -> itemService.getItemById("I1"));
        }

        @Test void getItemById_success_ReturnsItem() {
            ItemDTO dto = new ItemDTO(); dto.setId("I1"); dto.setItemType("VEHICLE");
            when(itemDAO.getItemById("I1")).thenReturn(dto);

            Item item = itemService.getItemById("I1");
            assertNotNull(item);
            assertEquals("I1", item.getId());
        }
    }

    // ==========================================
    // 8. GET ITEM BY NAME TESTS (51-54)
    // ==========================================
    @Nested
    class GetItemByNameTests {
        @Test void getItemByName_nameNullOrBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemByName(null));
            assertThrows(AuctionException.class, () -> itemService.getItemByName("   "));
        }

        @Test void getItemByName_emptyList_ThrowsException() {
            when(itemDAO.getItemByName("Laptop")).thenReturn(Collections.emptyList());
            assertThrows(ItemNotFoundException.class, () -> itemService.getItemByName("Laptop"));
        }

        @Test void getItemByName_success_ReturnsMappedItems() {
            ItemDTO dto = new ItemDTO(); dto.setItemType("ELECTRONICS");
            when(itemDAO.getItemByName("Laptop")).thenReturn(List.of(dto, dto));

            List<Item> items = itemService.getItemByName("Laptop");
            assertEquals(2, items.size());
        }
    }


    //GET ITEM BY TYPE

    @Nested
    class GetItemByTypeTests {
        @Test void getItemByType_typeNullOrBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.getItemByItemType(null));
            assertThrows(AuctionException.class, () -> itemService.getItemByItemType("   "));
        }

        @Test void getItemByType_emptyList_ThrowsException() {
            when(itemDAO.getItemByItemType("ART")).thenReturn(Collections.emptyList());
            assertThrows(ItemNotFoundException.class, () -> itemService.getItemByItemType("ART"));
        }

        @Test void getItemByType_success_ReturnsMappedItems() {
            ItemDTO dto = new ItemDTO(); dto.setItemType("ART");
            when(itemDAO.getItemByItemType("ART")).thenReturn(List.of(dto));

            List<Item> items = itemService.getItemByItemType("ART");
            assertEquals(1, items.size());
        }
    }


    //REMOVE ITEM

    @Nested
    class RemoveItemTests {
        @Test void removeItem_idNullOrBlank_ThrowsException() {
            assertThrows(AuctionException.class, () -> itemService.removeItem(null));
            assertThrows(AuctionException.class, () -> itemService.removeItem("  "));
        }

        @Test void removeItem_notFound_ThrowsException() {
            when(itemDAO.getItemById("I1")).thenReturn(null);
            assertThrows(ItemNotFoundException.class, () -> itemService.removeItem("I1"));
        }

        @Test void removeItem_hasSession_ThrowsException() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            when(sessionDAO.existsSessionByItemId("I1")).thenReturn(true);

            AuctionException e = assertThrows(AuctionException.class, () -> itemService.removeItem("I1"));
            assertTrue(e.getMessage().contains("already been used in an auction session"));
        }

        @Test void removeItem_success_CallsDelete() {
            when(itemDAO.getItemById("I1")).thenReturn(new ItemDTO());
            when(sessionDAO.existsSessionByItemId("I1")).thenReturn(false);

            assertDoesNotThrow(() -> itemService.removeItem("I1"));
            verify(itemDAO, times(1)).deleteItem("I1");
        }
    }
}