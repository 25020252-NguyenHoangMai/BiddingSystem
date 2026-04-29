#  BÀI TẬP LỚN - PHÁT TRIỂN HỆ THỐNG ĐẤU GIÁ TRỰC TUYẾN
Bài tập lớn Nhóm 14 

##  1. Phân công nhiệm vụ nhóm
* **Quỳnh :** Thiết kế cấu trúc Thực thể (Model), hệ thống xử lý Ngoại lệ (Exception) và viết Unit Test (JUnit 5).
* **Mai:** Phụ trách Tầng truy cập dữ liệu (DAO Layer), thiết kế và tương tác trực tiếp với cơ sở dữ liệu SQL Server, quy hoạch kiến trúc dự án.
* **Chi:** Phụ trách Tầng nghiệp vụ (Service Layer), kiểm duyệt dữ liệu (Validation), xử lý logic Đăng ký, Đăng nhập và mã hóa bảo mật BCrypt.
* **Hưng:** Xây dựng Giao diện người dùng bằng JavaFX (Client MVC) và xử lý luồng kết nối Socket phía người dùng.

---
##  2. Cấu trúc thư mục dự án



```text
BiddingSystem/
├── src/main/java/com/auction/
│   ├── client/                  # Module Giao diện người dùng (MVC Pattern)
│   │   ├── controller/          # Điều khiển giao diện (LoginController, MainController...)
│   │   ├── network/             # Quản lý kết nối Socket phía Client (ClientSocket)
│   │   └── util/                # Tiện ích chuyển đổi màn hình (SceneUtil)
│   │
│   ├── exception/               # Hệ thống Ngoại lệ tự định nghĩa (AuctionException, ItemNotFound...)
│   ├── model/                   # Lớp Thực thể đại diện dữ liệu (User, Seller, Item, Electronics...)
│   │
│   ├── request/                 # Data Transfer Object (DTO) gửi từ Client -> Server (LoginRequest...)
│   ├── response/                # Data Transfer Object (DTO) nhận từ Server -> Client (LoginResponse...)
│   │
│   └── server/                  # Module Xử lý trung tâm phía Máy chủ
│       ├── controller/          # Tiếp nhận & luân chuyển Request từ Client (AuthController...)
│       ├── dao/                 # Tầng giao tiếp trực tiếp SQL Server (UserDAO, ItemDAO, DatabaseManager)
│       ├── dto/                 # DTO nội bộ phục vụ việc truy xuất Database (UserDTO, ItemDTO)
│       ├── factory/             # Tầng khởi tạo đối tượng chuẩn Factory Pattern (ItemFactory...)
│       ├── network/             # Quản lý Socket đa luồng (SocketServer, ClientHandler)
│       └── service/             # Xử lý logic nghiệp vụ, chặn lỗi Validation (UserService, ItemService)
│
├── src/main/resources/          # Chứa tài nguyên tĩnh của dự án
│   ├── views/                   # Giao diện FXML (Login.fxml, Main.fxml...)
│   └── schema.sql               # Kịch bản khởi tạo Cơ sở dữ liệu SQL Server
│
├── src/test/java/               # Bộ Kiểm thử đơn vị (Unit Test - JUnit 5)
│   └── com/auction/server/      # Test logic tầng Service và kết nối DAO
│
└── pom.xml                      # Cấu hình Maven (Quản lý thư viện BCrypt, JUnit, JavaFX)
\
```


    