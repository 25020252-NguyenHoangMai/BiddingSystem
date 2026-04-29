package com.auction.server.service;

import org.mindrot.jbcrypt.BCrypt;
import com.auction.exception.AuctionException;
import com.auction.exception.AuthenticationException;
import com.auction.exception.UserNotFoundException;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserDAO userDAO; // Giả lập tầng Database

    private UserService userService; // Nhét cái DAO giả vào UserService thật

    // 1 cái để đăng ký, 1 cái là giả lập từ DB trả về
    private User testUser;
    private User testUserInDB;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDAO);
        // 1. Chuẩn bị User mới tinh (Mật khẩu chưa băm - dùng để test Register)
        testUser = new Bidder();
        testUser.setId("U01");
        testUser.setUsername("quynh_admin");
        testUser.setPassword("password123");
        testUser.setFullName("Quỳnh");
        testUser.setRole("BIDDER");

        // 2. Chuẩn bị User đã nằm trong Database (Mật khẩu ĐÃ BĂM - dùng để test Login)
        testUserInDB = new Bidder();
        testUserInDB.setId("U01");
        testUserInDB.setUsername("quynh_admin");
        testUserInDB.setFullName("Quỳnh");
        testUserInDB.setRole("BIDDER");
        testUserInDB.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));
    }


    // ========================================================
    // 1. TEST REGISTER
    // ========================================================

    @Test
    void testRegister_UsernameDaTonTai_PhaiNemLoi() {
        // TH1: tên đã có người dùng
        when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

        assertThrows(AuctionException.class, () -> {
            userService.register(testUser);
        }, "Lỗi: Đăng ký trùng username mà không ném lỗi AuctionException!");

        // Đảm bảo hàm insertUser không bao giờ được gọi
        verify(userDAO, never()).insertUser(any());
    }

    @Test
    void testRegister_ThanhCong() {
        // TH2: Tên chưa ai dùng (trả về null)
        when(userDAO.getUserByUsername("quynh_admin")).thenReturn(null);

        userService.register(testUser);

        // Kiểm tra xem ID đã được tự động sinh ra chưa
        assertNotNull(testUser.getId(), "Lỗi: ID chưa được sinh tự động!");
        // Kiểm tra xem mật khẩu đã bị băm chưa (Độ dài chuỗi băm BCrypt thường là 60 ký tự)
        assertEquals(60, testUser.getPassword().length(), "Lỗi: Mật khẩu chưa được băm!");

        // Đảm bảo hàm insertUser đã được gọi đúng 1 lần để lưu xuống DB
        verify(userDAO, times(1)).insertUser(testUser);
    }


    // ========================================================
    // 2. TEST LOGIN
    // ========================================================

    @Test
    void testLogin_UsernameKhongTonTai_PhaiNemLoi() {
        when(userDAO.getUserByUsername("user_ma")).thenReturn(null);

        assertThrows(AuthenticationException.class, () -> {
            userService.login("user_ma", "123456");
        });
    }

    @Test
    void testLogin_SaiMatKhau_PhaiNemLoi() {
        // Trả về User từ DB có mật khẩu đúng là "password123" (đã băm)
        when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

        assertThrows(AuthenticationException.class, () -> {
            // Cố tình truyền mật khẩu sai
            userService.login("quynh_admin", "sai_mat_khau");
        });
    }

    @Test
    void testLogin_ThanhCong() {
        when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

        User loggedInUser = userService.login("quynh_admin", "password123");

        assertNotNull(loggedInUser, "Lỗi: Đăng nhập thành công phải trả về Object User");
        assertEquals("quynh_admin", loggedInUser.getUsername());
        assertNull(loggedInUser.getPassword(), "Lỗi: Mật khẩu không được xóa để bảo mật trước khi trả về!");
    }


    // ========================================================
    // 3. TEST CHANGE PASSWORD
    // ========================================================

    @Test
    void testChangePassword_ThanhCong() {
        userService.changePassword("U01", "new_password_xịn");

        // Không test được kết quả trực tiếp, nên ta test xem UserService
        // có gọi DAO để update mật khẩu (đã băm) thành công không
        verify(userDAO, times(1)).updatePassword(eq("U01"), anyString());
    }


    // ========================================================
    // 4. TEST UPDATE PROFILE
    // ========================================================

    @Test
    void testUpdateProfile_UsernameDaTonTaiCuaNguoiKhac_PhaiNemLoi() {
        // Mình là U01, nhưng đổi username thành "mai_dao"
        // mà "mai_dao" lại là của người khác (ID là U02)
        User otherPerson = new Bidder(); // Đổi từ DTO sang Bidder
        otherPerson.setId("U02"); // ID khác mình
        otherPerson.setUsername("mai_dao");

        testUser.setUsername("mai_dao"); // thử đổi tên
        when(userDAO.getUserByUsername("mai_dao")).thenReturn(otherPerson);

        assertThrows(AuctionException.class, () -> {
            userService.updateProfile(testUser);
        });

        verify(userDAO, never()).updateUser(any());
    }

    @Test
    void testUpdateProfile_ThanhCong() {
        // đổi thành tên chưa ai dùng hoặc tên cũ
        when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

        userService.updateProfile(testUser);

        // Đảm bảo lệnh update được chạy
        verify(userDAO, times(1)).updateUser(testUser);
    }


    // ========================================================
    // 5. TEST GET USER (BY ID & USERNAME)
    // ========================================================

    @Test
    void testGetUserById_KhongTonTai_PhaiNemLoi() {
        when(userDAO.getUserById("ID_ẢO")).thenReturn(null);

        assertThrows(UserNotFoundException.class, () -> {
            userService.getUserById("ID_ẢO");
        });
    }

    @Test
    void testGetUserByUsername_ThanhCong() {
        when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

        User result = userService.getUserByUsername("quynh_admin");
        assertNotNull(result);
        assertEquals("quynh_admin", result.getUsername());
    }
}