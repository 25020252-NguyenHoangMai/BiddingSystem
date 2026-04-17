package com.auction.server.dao;

import org.junit.jupiter.api.Test;
import java.sql.Connection;
import java.sql.SQLException;
import static org.junit.jupiter.api.Assertions.*;

public class DatabaseManagerTest {

    @Test
    void testConnection() {
        System.out.println("Đang kiểm tra kết nối");
        try (Connection conn = DatabaseManager.getInstance().getConnection()) {
            assertNotNull(conn, "Lỗi, Connection bị null!");
            assertFalse(conn.isClosed(), "Kết nối đã bị đóng");
            System.out.println("Kết nối SQL Server thành công");
        } catch (SQLException e) {
            fail("Kết nối thất bại! Lỗi: " + e.getMessage());
        }
    }
}
