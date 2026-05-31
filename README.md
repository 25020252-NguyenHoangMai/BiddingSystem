# Hệ Thống Đấu Giá Trực Tuyến

> Bài tập lớn Nhóm 14

---

## 1. Mô Tả Bài Toán

Xây dựng hệ thống đấu giá trực tuyến hoạt động theo thời gian thực thông qua kết nối TCP Socket, đảm bảo tính cập nhật và chính xác tuyệt đối cho các phiên đấu giá.

Phạm vi hệ thống bao gồm:

- **Người dùng:**
> Bidder: đăng ký, đăng nhập, chỉnh sửa thông tin cá nhân, nạp tiền, đặt giá thủ công hoặc tự động, theo dõi chi tiết các phiên đấu giá.
> 
> Seller (Bidder đã được kích hoạt quyền người bán): tạo phiên đấu giá, chỉnh sửa thông tin phiên đấu giá, hủy phiên đấu giá.
- **Admin:** quản lý người dùng, quản lý phiên đấu giá,hủy phiên đấu giá, vô hiệu hóa tài khoản người dùng.
- **Server:** xử lý logic nghiệp vụ, quản lý phiên đấu giá theo lịch, phát sự kiện realtime tới tất cả client đang kết nối.

---

## 2. Công Nghệ & Môi Trường

| Thành phần | Chi tiết |
|---|---|
| Ngôn ngữ | Java 25 |
| Giao diện | JavaFX 21 |
| Giao tiếp mạng | Java TCP Socket (port **5000**) |
| Cơ sở dữ liệu | SQL Server (MSSQL) |
| Connection Pool | HikariCP 5.1.0 |
| Serialization | Gson 2.10.1 |
| Mã hóa mật khẩu | jBCrypt 0.4 |
| Build tool | Maven 3.x |
| Unit Test | JUnit 5.10 + Mockito 5 |
| IDE khuyến nghị | IntelliJ IDEA |

### Yêu Cầu Cài Đặt

- **JDK 25**
- **SQL Server** đã tạo cơ sở dữ liệu bằng script tại đường dẫn `database/init.sql`
- **Cấu hình môi trường:** Truy cập vào phần **Edit Configurations** trên IntelliJ IDEA và thiết lập các biến môi trường (Environment Variables) sau (vui lòng thay đổi thông tin kết nối phù hợp với SQL Server trên máy của thầy/cô):
  * `DB_URL`: `jdbc:sqlserver://localhost:1433;databaseName=BiddingSystem;encrypt=true;trustServerCertificate=true`
  * `DB_USER`: Tài khoản SQL Server
  * `DB_PASSWORD`: Mật khẩu SQL Server

---

## 3. Cấu Trúc Thư Mục

```
BiddingSystem/
├── src/main/java/com/auction/
│   ├── client/                         # Module Client (JavaFX MVC)
│   │   ├── ClientLauncher.java         # Entry point khởi động Client
│   │   ├── ClientSession.java          # Lưu trạng thái phiên đăng nhập
│   │   ├── MainApp.java                # Khởi tạo JavaFX Application
│   │   ├── controller/                 # Controller giao diện (Login, Main, AuctionDetail, Admin...)
│   │   ├── event/                      # Event bus nội bộ (ProfileUpdateBus)
│   │   ├── network/                    # ClientSocket — quản lý kết nối TCP tới Server
│   │   ├── service/                    # Gọi request & xử lý response (AuthService, AuctionService...)
│   │   └── util/                       # Tiện ích: SceneUtil, ImageCache, BidHistoryFormatter
│   │
│   ├── dto/                            # Data Transfer Object dùng chung (ItemDTO, UserSessionDTO...)
│   ├── exception/                      # Hệ thống ngoại lệ tùy chỉnh (InvalidBidException, InsufficientBalance...)
│   ├── model/                          # Domain model (User, Bidder, Admin, Item, AuctionSession, AutoBid...)
│   ├── protocol/                       # Giao thức truyền tin (BaseMessage, RequestMessage, ResponseMessage, EventMessage)
│   ├── request/                        # Các lớp Request (LoginRequest, PlaceBidRequest, SetAutoBidRequest...)
│   ├── response/                       # Các lớp Response (LoginResponse, BidUpdateResponse, DashboardUpdateResponse...)
│   │
│   └── server/                         # Module Server
│       ├── MainServer.java             # Entry point khởi động Server
│       ├── controller/                 # Phân luồng xử lý Request (AuthController, BiddingController, AuctionController...)
│       ├── dao/                        # Tầng truy cập dữ liệu (UserDAO, ItemDAO, BidDAO, SessionDAO, AutoBidDAO)
│       ├── factory/                    # Factory Pattern tạo Item (ArtFactory, VehicleFactory, ElectronicsFactory...)
│       ├── network/                    # SocketServer đa luồng + ClientHandler
│       ├── realtime/                   # Observer Pattern — đẩy sự kiện realtime tới client (DashboardWatchRegistry, SessionWatchRegistry)
│       └── service/                    # Logic nghiệp vụ (BiddingService, UserService, ItemService, AutoBiddingService, AntiSnipingService...)
│
├── src/main/resources/
│   └── views/                          # File giao diện FXML
│
├── src/test/java/                      # Unit Test (JUnit 5 + Mockito)
├── database/
│   └── init.sql                        # Script khởi tạo schema SQL Server
├── target/
│   ├── BiddingServer.jar               # ← File JAR chạy Server
│   └── BiddingClient.jar               # ← File JAR chạy Client
└── pom.xml
```

---

## 4. Vị Trí File `.jar`

Sau khi build bằng Maven (`mvn clean package`), hai file JAR thực thi nằm tại:

| File | Đường dẫn | Mô tả |
|---|---|---|
| `BiddingServer.jar` | `target/BiddingServer.jar` | Máy chủ — xử lý toàn bộ logic và kết nối DB |
| `BiddingClient.jar` | `target/BiddingClient.jar` | Ứng dụng client JavaFX dành cho người dùng |

---

## 5. Hướng Dẫn Chạy

### Bước 0 — Chuẩn bị Database

1. Mở SQL Server Management Studio (hoặc công cụ tương đương).
2. Chạy toàn bộ nội dung file `database/init.sql` để tạo schema và dữ liệu mẫu.
3. *Lưu ý:* Không cần chỉnh sửa bất kỳ dòng code nào trong file `DatabaseManager.java`. Thông tin tài khoản và mật khẩu kết nối sẽ được cấu hình linh hoạt ở bước khởi chạy ứng dụng phía dưới.
### Bước 1 — Khởi động Server *(chạy trước)*

```bash
$env:DB_URL="jdbc:sqlserver://localhost:1433;databaseName=BiddingSystem;encrypt=true;trustServerCertificate=true"; $env:DB_USER="tai_khoan_sql_server"; $env:DB_PASS="mat_khau_sql_server"; java -jar target/BiddingServer.jar
```

Server lắng nghe kết nối tại **port 5000**. Khi khởi động thành công sẽ in ra thông báo sẵn sàng nhận client.

*Lưu ý:* Thầy cô vui lòng chỉnh sửa lại giá trị của `$env:DB_USER` (tài khoản) và `$env:DB_PASS` (mật khẩu) trong câu lệnh trên thành đúng thông tin tài khoản SQL Server trên máy của thầy cô để hệ thống kết nối thành công. 
### Bước 2 — Khởi động Client *(chạy sau khi Server đã lên)*

```bash
java -jar target/BiddingClient.jar
```

> Có thể mở nhiều cửa sổ Client để mô phỏng nhiều người dùng cùng tham gia đấu giá.

---

## 6. Danh Sách Chức Năng Đã Hoàn Thành

### Xác thực & Tài khoản
- [x] Đăng ký tài khoản (mã hóa mật khẩu BCrypt)
- [x] Đăng nhập / Đăng xuất
- [x] Xem và chỉnh sửa hồ sơ cá nhân
- [x] Nạp tiền vào tài khoản

### Giao diện Đấu Giá (Bidder)
- [x] Xem danh sách tất cả phiên đấu giá đang diễn ra (realtime)
- [x] Xem chi tiết phiên đấu giá và lịch sử giá thầu (realtime)
- [x] Đặt giá thầu thủ công (Manual Bid)
- [x] Thiết lập đặt giá thầu tự động (Auto Bid / Proxy Bid)
- [x] Tắt chế độ Auto Bid
- [x] Cơ chế **Anti-Sniping** — tự động gia hạn thời gian phiên đấu khi có giá thầu sát giờ cuối
- [x] Trực quan hóa lịch sử giá thầu bằng biểu đồ đường (Line Chart) (realtime)
- [x] Xem lịch sử tham gia đấu giá (Session History)

### Giao diện Người Bán (Seller)
- [x] Đăng sản phẩm mới lên sàn
- [x] Chỉnh sửa thông tin sản phẩm đang đấu giá
- [x] Cập nhật thời gian kết thúc phiên đấu giá
- [x] Hủy phiên đấu giá của mình
- [x] Xem lịch sử bán hàng

### Giao diện Admin
- [x] Xem danh sách toàn bộ người dùng và quản lý trạng thái tài khoản
- [x] Vô hiệu hóa người dùng vi phạm
- [x] Xem toàn bộ danh sách phiên đấu giá trên hệ thống
- [x] Hủy phiên đấu giá

### Hạ Tầng, Kiến Trúc & Chất Lượng Mã Nguồn
- [x] Thiết kế kiến trúc Client-Server tách biệt, giao tiếp qua TCP Socket đa luồng (Multi-threading)
- [x] Hệ thống sự kiện realtime dựa trên nâng cao cấu trúc **Observer Pattern** (không sử dụng polling liên tục)
- [x] Xử lý cơ chế đấu giá đồng thời (Concurrent Bidding), đảm bảo Thread-safe, ngăn chặn Lost Update và Race Condition
- [x] Lập lịch tự động mở/đóng và xử lý trạng thái phiên đấu giá (AuctionSessionScheduler)
- [x] Áp dụng các Design Pattern cốt lõi: **Singleton** (Database connection), **Factory Method** (Tạo danh mục sản phẩm)
- [x] Mô hình phân tách rõ ràng áp dụng MVC (JavaFX + FXML ở Client; Controller, Model, DAO ở Server)
- [x] Hệ thống kiểm thử tự động (Unit Test) toàn diện cho các logic nghiệp vụ quan trọng với **JUnit 5** và **Mockito**
- [x] Tích hợp hệ thống **CI/CD tự động bằng GitHub Actions** để tự động kiểm thử mỗi khi commit code

### Tính Năng Sáng Tạo
- [x] **Smart Wallet Lock:** Tự động phân tách số dư tài khoản thành "Số dư khả dụng" và "Số dư đóng băng" theo từng lượt đặt giá, giúp bảo chứng giao dịch và tránh rủi ro hủy kèo.
---

# Link Báo Cáo PDF
```
https://drive.google.com/file/d/1PSqsRmPx_GIGCdY_aSNQbJddDc48Yn5o/view?usp=sharing
```

# Link Video Demo
```
https://drive.google.com/file/d/11n6VHqwPrj3KeJhMccI4RT8qmoCfgN2W/view?fbclid=IwY2xjawSI9DxleHRuA2FlbQIxMQBzcnRjBmFwcF9pZAEwAAEeUHDuFBxWf3J9ppTW3vKgIzY_KRx1Gn6q4cLTFhgGbnYHQgKb8A5nPaiV770_aem_etlmZb4-_yFexyFpVQDBfg
```
