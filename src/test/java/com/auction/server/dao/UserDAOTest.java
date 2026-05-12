package com.auction.server.dao;

import com.auction.exception.AuctionException;
import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.server.service.UserBalance;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
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

public class UserDAOTest {

    private UserDAO userDAO;

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
        userDAO = new UserDAO();

        mockedStaticDbManager = mockStatic(DatabaseManager.class);
        mockedStaticDbManager.when(DatabaseManager::getInstance).thenReturn(mockDbManager);
        when(mockDbManager.getConnection()).thenReturn(mockConn);
        when(mockConn.prepareStatement(anyString())).thenReturn(mockPs);
    }

    @AfterEach
    void tearDown() {
        mockedStaticDbManager.close();
    }


    //TEST MAP TO USER GIÁN TIẾP QUA GET USER

    @Nested
    class TestGetUserById {//mapping to user

        /*vì hàm mapToUser trong userDao là private không xâm nhập đc kể cả khi test
         * nên t dùng gián tiếp qua get by id */
        @Test
        void AdminRole_ReturnsAdmin() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getString("id")).thenReturn("A1");
            when(mockRs.getString("role")).thenReturn("ADMIN");
            when(mockRs.getString("username")).thenReturn("admin_minh");

            User result = userDAO.getUserById("A1");
            assertTrue(result instanceof Admin);
            assertEquals("admin_minh", result.getUsername());
        }

        @Test
        void BidderRole_ReturnsBidder() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getString("id")).thenReturn("B1");
            when(mockRs.getString("role")).thenReturn("BIDDER");
            when(mockRs.getDouble("balance")).thenReturn(1000.0);
            when(mockRs.getDouble("reservedBalance")).thenReturn(200.0);
            when(mockRs.getBoolean("sellerEnabled")).thenReturn(true);

            User result = userDAO.getUserById("B1");
            assertTrue(result instanceof Bidder);
            Bidder bidder = (Bidder) result;
            assertEquals(1000.0, bidder.getBalance());
            assertEquals(200.0, bidder.getReservedBalance());
            assertTrue(bidder.isSellerEnabled());
        }

        @Test
        void InvalidRole_ReturnsNull() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getString("role")).thenReturn("UNKNOWN_ROLE");

            assertNull(userDAO.getUserById("X1"));
        }

        @Test
        void NotFound_ReturnsNull() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);

            assertNull(userDAO.getUserById("GHOST"));
        }

        @Test
        void whenSQLExceptionOccurs_ThrowAuctionException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("DB Error"));
            assertThrows(AuctionException.class, () -> userDAO.getUserById("U1"));
        }

    }

    //TEST KIỂM TRA USERNAME VÀ GET TẤT CẢ USER


    @Nested
    class TestIsUsernameExist {
        @Test
        void Exists_ReturnsTrue() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            assertTrue(userDAO.isUsernameExist("test_user"));
        }

        @Test
        void NotExists_ReturnsFalse() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);
            assertFalse(userDAO.isUsernameExist("ghost_user"));
        }

        @Test
        void whenSQLExceptionOccurs_ThrowAuctionException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("Lỗi"));
            assertThrows(AuctionException.class, () -> userDAO.isUsernameExist("test"));
        }
    }


    @Nested
    class TestGetAllUsers {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true, false); // 1 user
            when(mockRs.getString("role")).thenReturn("ADMIN");

            List<User> list = userDAO.getAllUsers();
            assertEquals(1, list.size());
        }

        @Test
        void whenSQLExceptionOccurs_ThrowAuctionException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("Error"));
            assertThrows(AuctionException.class, () -> userDAO.getAllUsers());
        }
    }


    @Nested
    class TestGetUserByUsername {
        @Test
        void Success() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getString("role")).thenReturn("ADMIN");

            assertNotNull(userDAO.getUserByUsername("admin1"));
        }

        @Test
        void NotFound() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);
            assertNull(userDAO.getUserByUsername("admin1"));
        }

        @Test
        void whenSQLExceptionOccurs_ThrowAuctionException() throws SQLException {
            when(mockPs.executeQuery()).thenThrow(new SQLException("Error"));
            assertThrows(AuctionException.class, () -> userDAO.getUserByUsername("admin"));
        }
    }

    //TEST INSERT, UPDATE, DELETE


    @Nested
    class TestInsertUser {

        @Test
        void Success() throws SQLException {
            Bidder bidder = new Bidder("U1", "user", "pass", "Name", "BIDDER", 0, 0);
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.insertUser(bidder));
            verify(mockPs).setBoolean(8, false); //đảm bảo sellerEnabled = false
        }

        @Test
        void whenSQLExceptionOccurs_ThrowAuctionException() throws SQLException {
            Bidder bidder = new Bidder("U1", "user", "pass", "Name", "BIDDER", 0, 0);
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Duplicate PK"));
            assertThrows(AuctionException.class, () -> userDAO.insertUser(bidder));
        }
    }

    @Nested
    class TestUpdateUser {

        @Test
        void Success() throws SQLException {
            Bidder bidder = new Bidder("U1", "new_user", "pass", "New Name", "BIDDER", 0, 0);
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.updateUser(bidder));
        }

        @Test
        void whenSQLExceptionOccurs_ThrowAuctionException() throws SQLException {
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Error"));
            assertThrows(AuctionException.class, () -> userDAO.updateUser(new Bidder("U1", "", "", "", "", 0, 0)));
        }
    }


    @Nested
    class TestUpdatePassword {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.updatePassword("U1", "newPass123"));
        }

        @Test
        void NotFound_ThrowsException() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0);
            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.updatePassword("U1", "newPass"));
            assertEquals("User not found.", e.getMessage());
        }
    }


    @Nested
    class TestDeleteUser {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.deleteUser("U1"));
        }

        @Test
        void whenSQLExceptionOccurs_ThrowAuctionException() throws SQLException {
            when(mockPs.executeUpdate()).thenThrow(new SQLException("Error"));
            assertThrows(AuctionException.class, () -> userDAO.deleteUser("U1"));
        }
    }


    @Nested
    class TestEnableSeller {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            //do hàm enableSeller có gọi getUserById ở dòng return, cần mock thêm
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getString("role")).thenReturn("BIDDER");

            assertDoesNotThrow(() -> userDAO.enableSeller("U1"));
        }

        @Test
        void NotFound_ThrowsException() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0);
            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.enableSeller("U1"));
            assertEquals("User not found or user is not a bidder.", e.getMessage());
        }
    }

    //TEST NGHIỆP VỤ TIỀN BẠC

    @Nested
    class TestGetAvailableBalance {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getDouble("balance")).thenReturn(1000.0);
            when(mockRs.getDouble("reservedBalance")).thenReturn(300.0);

            assertEquals(700.0, userDAO.getAvailableBalance("U1"));
        }

        @Test
        void NotFound() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);

            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.getAvailableBalance("U1"));
            assertEquals("User not found.", e.getMessage());
        }
    }


    @Nested
    class TestGetBalanceForUpdate {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(true);
            when(mockRs.getDouble("balance")).thenReturn(500.0);
            when(mockRs.getDouble("reservedBalance")).thenReturn(100.0);

            UserBalance ub = userDAO.getBalanceForUpdate(mockConn, "U1");
            assertNotNull(ub);
        }

        @Test
        void NotFound() throws SQLException {
            when(mockPs.executeQuery()).thenReturn(mockRs);
            when(mockRs.next()).thenReturn(false);

            assertThrows(AuctionException.class, () -> userDAO.getBalanceForUpdate(mockConn, "U1"));
        }
    }


    @Nested
    class TestUpdateBalance {

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.updateBalance("U1", 500.0));
        }

        @Test
        void InsufficientOrNotFound() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0); //trừ quá số tiền hoặc user ko tồn tại
            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.updateBalance("U1", -5000.0));
            assertEquals("Insufficient balance or user not found.", e.getMessage());
        }
    }


    @Nested
    class TestReserveBalance {//ReserveBalance: số dư đóng băng

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.reserveBalance("U1", 200.0));
        }

        @Test
        void Insufficient() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0);
            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.reserveBalance("U1", 9999.0));
            assertEquals("Insufficient available balance.", e.getMessage());
        }
    }


    @Nested
    class TestUpdateReservedBalance { //UpdateReservedBalance: cập nhật số dư bị đóng băng

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.updateReservedBalance(mockConn, "U1", 150.0));
        }

        @Test
        void NotFound() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0);
            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.updateReservedBalance(mockConn, "U1", 150.0));
            assertEquals("User not found.", e.getMessage());
        }
    }


    @Nested
    class TestReleaseReservedBalance {//ReleaseReservedBalance: giải phóng số dư đặt trước

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.releaseReservedBalance("U1", 100.0));
        }

        @Test
        void Invalid() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0); //cố tình nhả số tiền lớn hơn số đang bị đóng băng
            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.releaseReservedBalance("U1", 999.0));
            assertEquals("Invalid reserved balance.", e.getMessage());
        }
    }


    @Nested
    class TestDeductReservedBalance {//DeductReservedBalance: trừ đi số dư đóng băng

        @Test
        void Success() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(1);
            assertDoesNotThrow(() -> userDAO.deductReservedBalance("U1", 100.0));
        }

        @Test
        void Invalid() throws SQLException {
            when(mockPs.executeUpdate()).thenReturn(0);
            AuctionException e = assertThrows(AuctionException.class, () -> userDAO.deductReservedBalance("U1", 999.0));
            assertEquals("Invalid reserved balance or user not found.", e.getMessage());
        }
    }
}