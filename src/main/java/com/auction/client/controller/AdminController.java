package com.auction.client.controller;

import com.auction.client.service.ProductService;
import com.auction.client.service.UserClientService;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import javafx.beans.property.SimpleObjectProperty;
import javafx.beans.property.SimpleStringProperty;
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
    @FXML private TableColumn<ItemDTO, Boolean> colItemSelect;
    @FXML private TableColumn<ItemDTO, String> colItemId;
    @FXML private TableColumn<ItemDTO, String> colItemName;
    @FXML private TableColumn<ItemDTO, String> colItemSeller;
    private final ProductService productService = ProductService.getInstance();

    @FXML private TextField txtSearchItem;
    @FXML private VBox itemDetailContainer;
    @FXML private Button btnDeleteItem;
    @FXML private Button btnEditItem;

    // --- PHẦN QUẢN LÝ NGƯỜI DÙNG ---
    @FXML private TableView<UserSessionDTO> userTable;
    @FXML private TableColumn<UserSessionDTO, Boolean> colUserSelect;
    @FXML private TableColumn<UserSessionDTO, String> colUserId;
    @FXML private TableColumn<UserSessionDTO, String> colUsername;
    private final UserClientService userService = UserClientService.getInstance();

    @FXML private TextField txtSearchUser;
    @FXML private VBox userDetailContainer;
    @FXML private Button btnDeleteUser;
    @FXML private Button btnEditUser;

    private final ObservableList<ItemDTO> masterDataItems = FXCollections.observableArrayList();
    private final ObservableList<UserSessionDTO> masterDataUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Cấu hình bảng Item
        // Lùng Lambda để báo lỗi viết sai tên hàm 
        colItemId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId())); //(new PropertyValueFactory<>("id"));
        colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName())); //(new PropertyValueFactory<>("name"));
        colItemSeller.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSellerUsername()));//(new PropertyValueFactory<>("sellerUsername"));
        colItemSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colItemSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colItemSelect));
        itemTable.setEditable(true);

        // Cấu hình bảng User
        // Dùng Lambda để báo lỗi viết sai tên hàm
        colUserId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId())); //(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername())); //(new PropertyValueFactory<>("username"));
        colUserSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colUserSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colUserSelect));
        userTable.setEditable(true);

        // Gắn list vào bảng
        itemTable.setItems(masterDataItems);
        userTable.setItems(masterDataUsers);

        loadDataFromServer();
        setupSearchFilters();
    }

    private void loadDataFromServer() {
        try {
            List<ItemDTO> products = productService.getAllProducts();
            List<UserSessionDTO> users = userService.getAllUsers();

            masterDataItems.setAll(products);
            masterDataUsers.setAll(users);

        } catch (Exception e) {
            System.err.println("Lỗi khi tải dữ liệu: " + e.getMessage());
        }
    }

    private void setupSearchFilters() {
        FilteredList<ItemDTO> filteredItems = new FilteredList<>(masterDataItems, p -> true);
        txtSearchItem.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredItems.setPredicate(item -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                if (item.getName() != null && item.getName().toLowerCase().contains(lowerCaseFilter)) return true;
                if (item.getSellerUsername() != null && item.getSellerUsername().toLowerCase().contains(lowerCaseFilter)) return true;

                return false;
            });
        });
        itemTable.setItems(filteredItems);

        FilteredList<UserSessionDTO> filteredUsers = new FilteredList<>(masterDataUsers, p -> true);
        txtSearchUser.textProperty().addListener((observable, oldValue, newValue) -> {
            filteredUsers.setPredicate(user -> {
                if (newValue == null || newValue.isEmpty()) return true;
                String lowerCaseFilter = newValue.toLowerCase();

                return user.getUsername() != null
                        && user.getUsername().toLowerCase().contains(lowerCaseFilter);
            });
        });
        userTable.setItems(filteredUsers);
    }
}




