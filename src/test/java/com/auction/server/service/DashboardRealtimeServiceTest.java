package com.auction.server.service;

import com.auction.dto.ItemDTO;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.DashboardUpdateType;
import com.auction.server.realtime.DashboardWatchRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class DashboardRealtimeServiceTest {

    @Mock
    private DashboardWatchRegistry dashboardWatchRegistry;
    @Mock
    private ItemService itemService;

    private DashboardRealtimeService dashboardRealtimeService;
    private ItemDTO mockItem;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        dashboardRealtimeService = new DashboardRealtimeService(dashboardWatchRegistry, itemService);

        mockItem = new ItemDTO();
        mockItem.setId("ITEM_1");
        mockItem.setName("Laptop Dell");
    }

    @Nested
    class BroadcastTests {

        @Test
        void broadcastItemAdded_Success() {
            dashboardRealtimeService.broadcastItemAdded(mockItem, "New item added");

            //ArgumentCaptor(class thuộc thư viện mock): để bắt thứ truyền vào method của mock ktra xem đúng hay k
            ArgumentCaptor<DashboardUpdateResponse> captor = ArgumentCaptor.forClass(DashboardUpdateResponse.class);
            verify(dashboardWatchRegistry).broadcastDashboardUpdate(captor.capture());

            DashboardUpdateResponse response = captor.getValue();
            assertTrue(response.isSuccess());
            assertEquals("New item added", response.getMessage());
            assertEquals(DashboardUpdateType.ITEM_ADDED, response.getType());
            assertEquals(mockItem, response.getItem());
        }

        @Test
        void broadcastItemUpdated_Success() {
            dashboardRealtimeService.broadcastItemUpdated(mockItem, "Item updated");

            ArgumentCaptor<DashboardUpdateResponse> captor = ArgumentCaptor.forClass(DashboardUpdateResponse.class);
            verify(dashboardWatchRegistry).broadcastDashboardUpdate(captor.capture());

            DashboardUpdateResponse response = captor.getValue();
            assertEquals(DashboardUpdateType.ITEM_UPDATED, response.getType());
            assertEquals("Item updated", response.getMessage());
        }

        @Test
        void broadcastItemRemoved_Success() {
            dashboardRealtimeService.broadcastItemRemoved(mockItem, "Item removed");

            ArgumentCaptor<DashboardUpdateResponse> captor = ArgumentCaptor.forClass(DashboardUpdateResponse.class);
            verify(dashboardWatchRegistry).broadcastDashboardUpdate(captor.capture());

            DashboardUpdateResponse response = captor.getValue();
            assertEquals(DashboardUpdateType.ITEM_REMOVED, response.getType());
        }

        @Test
        void broadcast_NullItem_DoesNothing() {
            //hàm broadcast return luôn, không gọi registry
            dashboardRealtimeService.broadcastItemAdded(null, "Null item");
            verify(dashboardWatchRegistry, never()).broadcastDashboardUpdate(any());
        }
    }

    @Nested
    class BroadcastBySessionIdTests {

        @Test
        void success() {
            when(itemService.getAuctionDetailDTO("SESSION_1")).thenReturn(mockItem);

            dashboardRealtimeService.broadcastItemUpdatedBySessionId("SESSION_1", "Session updated");

            ArgumentCaptor<DashboardUpdateResponse> captor = ArgumentCaptor.forClass(DashboardUpdateResponse.class);
            verify(dashboardWatchRegistry).broadcastDashboardUpdate(captor.capture());

            assertEquals(DashboardUpdateType.ITEM_UPDATED, captor.getValue().getType());
            assertEquals(mockItem, captor.getValue().getItem());
        }
    }

    @Nested
    class BroadcastSellerItemsUpdatedTests {

        @Test
        void validSellerId_LoopsAndBroadcasts() {
            ItemDTO item2 = new ItemDTO();
            item2.setId("ITEM_2");
            List<ItemDTO> sellerItems = List.of(mockItem, item2);

            when(itemService.getDashboardItemDTOSBySellerId("nguyen_cong_minh")).thenReturn(sellerItems);

            dashboardRealtimeService.broadcastSellerItemsUpdated("nguyen_cong_minh", "Seller items updated");

            //có 2 items nên broadcastDashboardUpdate pk dc gọi đúng 2 lần
            verify(dashboardWatchRegistry, times(2)).broadcastDashboardUpdate(any(DashboardUpdateResponse.class));
        }

        @Test
        void nullOrBlankSellerId_DoesNothing() {
            dashboardRealtimeService.broadcastSellerItemsUpdated(null, "Test");
            dashboardRealtimeService.broadcastSellerItemsUpdated("   ", "Test");
            dashboardRealtimeService.broadcastSellerItemsUpdated("", "Test");

            verify(itemService, never()).getDashboardItemDTOSBySellerId(anyString());
            verify(dashboardWatchRegistry, never()).broadcastDashboardUpdate(any());
        }

        @Test
        void noItemsForSeller_DoesNothing() {
            when(itemService.getDashboardItemDTOSBySellerId("SELLER_1")).thenReturn(Collections.emptyList());

            dashboardRealtimeService.broadcastSellerItemsUpdated("SELLER_1", "Test");

            verify(dashboardWatchRegistry, never()).broadcastDashboardUpdate(any());
        }
    }
}