package com.auction.server.service;

import com.auction.exception.AuctionException;
import com.auction.exception.ItemNotFoundException;
import com.auction.model.Electronics;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class ItemServiceTest {

    //giá khởi điểm âm
    @Test
    void testAddItem_GiaKhoiDiemAm_PhaiNemLoi() {
        ItemService service = new ItemService();
        Seller seller = new Seller("U01", "seller1", "123", "Ông Bán Hàng");
        //tivi -500
        Electronics tiviLoi = new Electronics("I01", "Tivi Sony", "Mô tả", seller.getId(), -500000, "Sony");

        assertThrows(AuctionException.class, () -> {
            service.addItem(tiviLoi, seller);
        }, "Lỗi:giá sản phẩm âm!");
    }

    //tên sp trống
    @Test
    void testAddItem_TenSanPhamRong_PhaiNemLoi() {
        ItemService service = new ItemService();
        Seller seller = new Seller("U01", "seller1", "123", "Ông Bán Hàng");

        //cố tình để trống tên
        Electronics tiviLoi = new Electronics("I02", "", "Mô tả", seller.getId(), 1000000, "Sony");

        assertThrows(AuctionException.class, () -> {
            service.addItem(tiviLoi, seller);
        }, "Lỗi:sản phẩm phải có tên");
    }

    // tìm ID không tồn tại
    @Test
    void testGetItemById_KhongTonTai_PhaiNemLoi() {
        ItemService service = new ItemService();

        assertThrows(ItemNotFoundException.class, () -> {
            service.getItemById("ID_MA_QUY_12345");
        }, "Lỗi: Tìm sản phẩm không có thật mà không ném lỗi ItemNotFoundException!");
    }
}