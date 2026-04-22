package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.Bidder;
import com.auction.model.Electronics;
import com.auction.server.dao.ItemDAO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
public class ItemServiceTest {


    @Mock
    private ItemDAO itemDAO;

    @InjectMocks
    private ItemService service;

    // giá khởi điểm âm
    @Test
    void testAddItem_GiaKhoiDiemAm_PhaiNemLoi() {

        Bidder seller = new Bidder("U01", "seller1", "123", "Ông Bán Hàng", "BIDDER", 0);
        seller.enableSelling();

        Electronics tiviLoi = new Electronics("I01", "Tivi Sony", "Mô tả", seller.getId(), -500000, "Sony");

        assertThrows(AuctionException.class, () -> {
            service.addItem(tiviLoi, seller);
        }, "Lỗi:giá sản phẩm âm!");
    }

    // tên sp trống
    @Test
    void testAddItem_TenSanPhamRong_PhaiNemLoi() {
        Bidder seller = new Bidder("U01", "seller1", "123", "Ông Bán Hàng", "BIDDER", 0);
        seller.enableSelling();

        // cố tình để trống tên
        Electronics tiviLoi = new Electronics("I02", "", "Mô tả", seller.getId(), 1000000, "Sony");

        assertThrows(AuctionException.class, () -> {
            service.addItem(tiviLoi, seller);
        }, "Lỗi:sản phẩm phải có tên");
    }

    // tìm ID không tồn tại
    @Test
    void testGetItemById_KhongTonTai_PhaiNemLoi() {


        when(itemDAO.getItemById("ID_MA_QUY_12345")).thenReturn(null);

        assertThrows(ItemNotFoundException.class, () -> {
            service.getItemById("ID_MA_QUY_12345");
        }, "Lỗi: Tìm sản phẩm không có thật mà không ném lỗi ItemNotFoundException!");
    }
}