package com.auction.server.service;

import com.auction.model.AuctionSession;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.*;

public class AntiSnipingServiceTest {

    private AntiSnipingService antiSnipingService;

    @BeforeEach
    void setUp() {
        antiSnipingService = new AntiSnipingService();
    }

    @Test
    void testShouldExtend_SessionNull_ReturnsFalse() {
        assertFalse(antiSnipingService.shouldExtend(null), "Session null phải trả về false");
    }

    @Test
    void testShouldExtend_EndTimeNull_ReturnsFalse() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);
        //ko set endTime

        assertFalse(antiSnipingService.shouldExtend(session), "EndTime null phải trả về false");
    }

    @Test
    void testShouldExtend_StatusNotRunning_ReturnsFalse() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_FINISHED); //đã kết thúc
        session.setEndTime(LocalDateTime.now().plusSeconds(10));

        assertFalse(antiSnipingService.shouldExtend(session), "Status không phải RUNNING phải trả về false");
    }

    @Test
    void testShouldExtend_TimeAlreadyPassed_ReturnsFalse() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);
        session.setEndTime(LocalDateTime.now().minusSeconds(10));//đã qua 10 giây trc

        assertFalse(antiSnipingService.shouldExtend(session), "Thời gian đã trôi qua phải trả về false");
    }

    @Test
    void testShouldExtend_RemainingTimeGreaterThan30s_ReturnsFalse() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);
        session.setEndTime(LocalDateTime.now().plusSeconds(40)); //còn 40 giây

        assertFalse(antiSnipingService.shouldExtend(session), "Còn hơn 30 giây thì không được gia hạn");
    }

    @Test
    void testShouldExtend_RemainingTimeLessThan30s_ReturnsTrue() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);
        session.setEndTime(LocalDateTime.now().plusSeconds(15)); //chỉ còn 15 giây nằm trong vùng sniping

        assertTrue(antiSnipingService.shouldExtend(session), "Còn dưới 30 giây phải trả về true để gia hạn");
    }

    @Test
    void testShouldExtend_RemainingTimeExactly30s_ReturnsTrue() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);
        session.setEndTime(LocalDateTime.now().plusSeconds(30)); // Vừa khít 30 giây

        assertTrue(antiSnipingService.shouldExtend(session), "Còn đúng 30 giây phải trả về true");
    }


    @Test
    void testGetExtendTime_Returns60Seconds() {
        //đảm bảo không ai sửa thời gian gia hạn thành số khác
        assertEquals(Duration.ofSeconds(60), antiSnipingService.getExtendTime(),
                "Lỗi: Thời gian gia hạn mặc định phải chuẩn 60 giây!");
    }

    @Test
    void testGetAntisnipingThreshold_Returns30Seconds() {
        //đảm bảo ngưỡng kích hoạt bắn tỉa luôn là 30 giây
        assertEquals(Duration.ofSeconds(30), antiSnipingService.getAntisnipingThreshold(),
                "Lỗi: Ngưỡng chống bắn tỉa mặc định phải chuẩn 30 giây!");
    }


    @Test
    void testShouldExtend_StatusIsNull_ReturnsFalse_NoException() {
        AuctionSession session = new AuctionSession();
        session.setEndTime(LocalDateTime.now().plusSeconds(10));
        session.setStatus(null); //giả lập dữ liệu từ DB lên bị lỗi, không có trạng thái

        // xem code có bị văng lỗi NullPointerException ko
        assertDoesNotThrow(() -> {
            boolean result = antiSnipingService.shouldExtend(session);
            assertFalse(result, "Trạng thái null phải trả về false thay vì ném lỗi");
        }, "Lỗi: Hàm không bắt được NullPointerException khi status null!");
    }

    @Test
    void testShouldExtend_RemainingTimeIsOneMillisecond_ReturnsTrue() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);
        //set time kết thúc chỉ còn đúng 1 mili-giây
        session.setEndTime(LocalDateTime.now().plusNanos(1000000));

        assertTrue(antiSnipingService.shouldExtend(session),
                "Sát nút 1 mili-giây trước khi kết thúc vẫn phải tính là hợp lệ để gia hạn!");
    }


    @Test
    void testShouldExtend_NowEqualsEndTime_ReturnsFalse() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);

        //đặt giờ kết thúc chính là khoảnh khắc hiện tại (now == endTime)
        // khi code chạy vào hàm, 'now' bên trong hàm sẽ trễ hơn 1-2 mili-giây
        // =>sẽ rơi vào nhánh chặn (!isBefore)
        session.setEndTime(LocalDateTime.now());

        assertFalse(antiSnipingService.shouldExtend(session),
                "Đúng khoảnh khắc hết giờ thì không được gia hạn nữa!");
    }

    @Test
    void testShouldExtend_RemainingTimeExactly31s_ReturnsFalse() {
        AuctionSession session = new AuctionSession();
        session.setStatus(SessionService.STATUS_RUNNING);

        //cài time còn đúng 31 giây nằm ngay ngoài lề của mốc 30s
        session.setEndTime(LocalDateTime.now().plusSeconds(31));

        assertFalse(antiSnipingService.shouldExtend(session),
                "Còn đúng 31 giây nằm ngoài vùng bắn tỉa 30s thì KHÔNG được gia hạn!");
    }
}