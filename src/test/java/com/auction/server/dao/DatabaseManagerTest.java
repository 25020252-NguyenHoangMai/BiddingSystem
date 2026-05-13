package com.auction.server.dao;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class DatabaseManagerTest {

    @Mock
    private DataSource mockDataSource;

    @Mock
    private Connection mockConnection;

    @BeforeEach
    void setUp() throws SQLException {
        MockitoAnnotations.openMocks(this);

        when(mockDataSource.getConnection()).thenReturn(mockConnection);
        when(mockConnection.isClosed()).thenReturn(false);

        DatabaseManager.setTestInstance(mockDataSource);
    }

    @AfterEach
    void tearDown() {

        DatabaseManager.resetInstance();
    }

    @Test
    void testConnection() {

        try (Connection conn = DatabaseManager.getInstance().getConnection()) {

            assertNotNull(conn, "Lỗi, Connection bị null!");
            assertFalse(conn.isClosed(), "Kết nối đã bị đóng");

            System.out.println("Lấy kết nối thành công từ Mock DataSource");

            verify(mockDataSource, times(1)).getConnection();

        } catch (SQLException e) {
            fail("Kết nối thất bại! Lỗi: " + e.getMessage());
        }
    }


    @Nested
    class SingletonLifecycleTests {

        @Test
        void getInstance_ReturnsSameInstance() {

            DatabaseManager.setTestInstance(mockDataSource);

            DatabaseManager instance1 = DatabaseManager.getInstance();
            DatabaseManager instance2 = DatabaseManager.getInstance();

            assertSame(instance1, instance2, "Lỗi: Hệ thống sinh ra 2 ông Quản lý khác nhau!");
        }

        @Test
        void resetInstance_CreatesNewInstance() {

            DatabaseManager.setTestInstance(mockDataSource);
            DatabaseManager instance1 = DatabaseManager.getInstance();

            DatabaseManager.resetInstance();

            /*public static void setTestInstance(DataSource dataSource) {
                    instance = new DatabaseManager(dataSource);
              }
              đây là hàm trong database mângẻr của mai, vì new nên instance 2 sẽ là trỏ đến object mới
               nên phải khác nhau (sau reset phải là ông quản lý mới)
              */
            DatabaseManager.setTestInstance(mockDataSource);
            DatabaseManager instance2 = DatabaseManager.getInstance();

            assertNotSame(instance1, instance2, "Lỗi: resetInstance() không hoạt động, vẫn dùng Quản lý cũ!");
        }

        @Test
        void getInstance_WorksAfterReset() {

            DatabaseManager.setTestInstance(mockDataSource);
            DatabaseManager.resetInstance();

            DatabaseManager.setTestInstance(mockDataSource);
            assertDoesNotThrow(() -> {
                DatabaseManager newInstance = DatabaseManager.getInstance();
                assertNotNull(newInstance);
            });
        }
    }
}