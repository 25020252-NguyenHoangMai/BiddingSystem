package com.auction.server.service;

import org.junit.jupiter.api.Nested;
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
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
public class UserServiceTest {

    @Mock
    private UserDAO userDAO;

    private UserService userService;

    //1 cái để đăng ký, 1 cái là giả lập từ DB trả về
    private User testUser;
    private User testUserInDB;

    @BeforeEach
    void setUp() {
        userService = new UserService(userDAO);
        //user ms-mk chx băm
        testUser = new Bidder();
        testUser.setId("U01");
        testUser.setUsername("quynh_admin");
        testUser.setPassword("password123");
        testUser.setFullName("Quỳnh");
        testUser.setRole("BIDDER");

        //cbi user cũ mk đã băm
        testUserInDB = new Bidder();
        testUserInDB.setId("U01");
        testUserInDB.setUsername("quynh_admin");
        testUserInDB.setFullName("Quỳnh");
        testUserInDB.setRole("BIDDER");
        testUserInDB.setPassword(BCrypt.hashpw("password123", BCrypt.gensalt()));
    }


    @Nested
    class TestRegister {
        @Test
        void UsernameDaTonTai_PhaiNemLoi() {

            when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

            assertThrows(AuctionException.class, () -> {
                userService.register(testUser);
            }, "Lỗi: Đăng ký trùng username mà không ném lỗi AuctionException!");

            // Đảm bảo hàm insertUser không bao giờ được gọi
            verify(userDAO, never()).insertUser(any());
        }

        @Test
        void ThanhCong() {

            when(userDAO.getUserByUsername("quynh_admin")).thenReturn(null);

            userService.register(testUser);

            assertNotNull(testUser.getId(), "Lỗi: ID chưa được sinh tự động!");

            assertEquals(60, testUser.getPassword().length(), "Lỗi: Mật khẩu chưa được băm!");


            verify(userDAO, times(1)).insertUser(testUser);
        }
    }





    @Nested
    class testLogin {
        @Test
        void UsernameKhongTonTai_PhaiNemLoi() {
            when(userDAO.getUserByUsername("user_ma")).thenReturn(null);

            assertThrows(AuthenticationException.class, () -> {
                userService.login("user_ma", "123456");
            });
        }

        @Test
        void SaiMatKhau_PhaiNemLoi() {
            //trả về mk dungds (password123) đã băm
            when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

            assertThrows(AuthenticationException.class, () -> {
                userService.login("quynh_admin", "sai_mat_khau");//truyền sai mk
            });
        }

        @Test
        void ThanhCong() {
            when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);

            User loggedInUser = userService.login("quynh_admin", "password123");

            assertNotNull(loggedInUser, "Lỗi: Đăng nhập thành công phải trả về Object User");
            assertEquals("quynh_admin", loggedInUser.getUsername());
            assertNull(loggedInUser.getPassword(), "Lỗi: Mật khẩu không được xóa để bảo mật trước khi trả về!");
        }
    }





    @Test
    void testChangePassword_ThanhCong() {
        when(userDAO.getUserById("U01")).thenReturn(testUserInDB);
        userService.changePassword("U01", "new_password_xịn");

        verify(userDAO, times(1)).updatePassword(eq("U01"), anyString());
    }




    @Nested
    class testUpdateProfile {
        @Test
        void UsernameDaTonTaiCuaNguoiKhac_PhaiNemLoi() {
            when(userDAO.getUserById("U01")).thenReturn(testUserInDB);
            User otherPerson = new Bidder();
            otherPerson.setId("U02");
            otherPerson.setUsername("mai_dao");

            when(userDAO.getUserByUsername("mai_dao")).thenReturn(otherPerson);

            assertThrows(AuctionException.class, () -> {
                userService.updateProfile("U01", "Quỳnh mới", "mai_dao", "new_pass");
            });

            verify(userDAO, never()).updateUser(any());
        }

        @Test
        void ThanhCong() {

            when(userDAO.getUserById("U01")).thenReturn(testUserInDB);
            when(userDAO.getUserByUsername("quynh_admin")).thenReturn(testUserInDB);
            when(userDAO.getUserById("U01")).thenReturn(testUserInDB);

            userService.updateProfile("U01", "Quỳnh mới", "quynh_admin", "new_pass");

            // Đảm bảo lệnh update được chạy
            verify(userDAO, times(1)).updateUser(any(User.class));
            verify(userDAO, times(1)).updatePassword(eq("U01"), anyString());
        }
    }



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