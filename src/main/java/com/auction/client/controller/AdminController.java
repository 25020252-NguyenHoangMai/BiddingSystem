package com.auction.client.controller;

import com.auction.client.service.ProductService;
import com.auction.client.service.UserClientService;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.VBox;

import java.util.List;

public class AdminController {
    // --- PHẦN QUẢN LÝ SẢN PHẨM ---
    @FXML
    private TableView<ItemDTO> itemTable;
    @FXML private TableColumn<ItemDTO, Boolean> colItemSelect; // Cột tích chọn
    @FXML private TableColumn<ItemDTO, Integer> colItemId;
    @FXML private TableColumn<ItemDTO, String> colItemName;
    @FXML private TableColumn<ItemDTO, String> colItemSeller;
    @FXML private TableColumn<ItemDTO, String> colItemType;
    private final ProductService productService = ProductService.getInstance();

    @FXML private TextField txtSearchItem;
    @FXML private VBox itemDetailContainer; // Vùng hiện Artist, Engine...
    @FXML private Button btnDeleteItem;
    @FXML private Button btnEditItem;

    // --- PHẦN QUẢN LÝ NGƯỜI DÙNG ---
    @FXML private TableView<UserSessionDTO> userTable;
    @FXML private TableColumn<UserSessionDTO, Boolean> colUserSelect; // Cột tích chọn
    @FXML private TableColumn<UserSessionDTO, Integer> colUserId;
    @FXML private TableColumn<UserSessionDTO, String> colUsername;
    private final UserClientService userService = UserClientService.getInstance();

    @FXML private TextField txtSearchUser;
    @FXML private VBox userDetailContainer; // Vùng hiện thông tin User
    @FXML private Button btnDeleteUser;
    @FXML private Button btnEditUser;

    private final ObservableList<ItemDTO> masterDataItems = FXCollections.observableArrayList();
    private final ObservableList<UserSessionDTO> masterDataUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // 1. Cấu hình các cột cho bảng User
        colUserId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));

        // 2. Cấu hình cột Checkbox (Để tích chọn nhiều dòng)
        colUserSelect.setCellValueFactory(new PropertyValueFactory<>("selected"));
        colUserSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colUserSelect));
        userTable.setEditable(true);

        // 3. Gắn list vào bảng
        userTable.setItems(masterDataUsers);
        itemTable.setItems(masterDataItems);

        // 4. Lắng nghe sự kiện tìm kiếm (Filter)
        loadDataFromServer();
        setupSearchFilters();
    }

    private void loadDataFromServer() {
        try {
            // Lấy danh sách từ Service
            List<ItemDTO> products = productService.getAllProducts();
            List<UserSessionDTO> users = userService.getAllUsers();

            // Cập nhật vào ObservableList
            // Dùng setAll để thông báo cho TableView vẽ lại mà không làm mất liên kết Filter
            masterDataItems.setAll(products);
            masterDataUsers.setAll(users);

        } catch (Exception e) {
            // Hiện thông báo lỗi nếu không kết nối được Socket
            System.err.println("Lỗi khi tải dữ liệu: " + e.getMessage());
            // Có thể hiện Alert ở đây để Admin biết
        }
    }

    private void setupSearchFilters() {
        // 1. Tạo bộ lọc cho bảng Sản phẩm
        FilteredList<ItemDTO> filteredItems = new FilteredList<>(masterDataItems, p -> true);
        txtSearchItem.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredItems.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                // Tìm theo tên sản phẩm hoặc tên người bán
                if (item.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (item.getSellerUsername() != null && item.getSellerUsername().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });
        itemTable.setItems(filteredItems);

        // 2. Tạo bộ lọc cho bảng Người dùng
        FilteredList<UserSessionDTO> filteredUsers = new FilteredList<>(masterDataUsers, p -> true);
        txtSearchUser.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUsers.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                // Tìm theo tên đăng nhập
                return user.getUsername().toLowerCase().contains(lowerCaseFilter);
            });
        });
        userTable.setItems(filteredUsers);
    }
}

