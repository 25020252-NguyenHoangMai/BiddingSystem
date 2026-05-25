package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.service.ProductService;
import com.auction.client.service.UserClientService;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.CheckBoxTableCell;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;

import java.util.List;
import java.util.Objects;

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

    // --- PHẦN QUẢN LÝ NGƯỜI DÙNG ---
    @FXML private TableView<UserSessionDTO> userTable;
    @FXML private TableColumn<UserSessionDTO, Boolean> colUserSelect;
    @FXML private TableColumn<UserSessionDTO, String> colUserId;
    @FXML private TableColumn<UserSessionDTO, String> colUsername;
    private final UserClientService userService = UserClientService.getInstance();

    @FXML private TextField txtSearchUser;
    @FXML private VBox userDetailContainer;
    @FXML private Button btnDeleteUser;

    private final ObservableList<ItemDTO> masterDataItems = FXCollections.observableArrayList();
    private final ObservableList<UserSessionDTO> masterDataUsers = FXCollections.observableArrayList();

    @FXML
    public void initialize() {
        // Cấu hình bảng Item
        // Lùng Lambda để báo lỗi viết sai tên hàm 
        colItemId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colItemName.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getName()));
        colItemSeller.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getSellerUsername()));
        colItemSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colItemSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colItemSelect));
        itemTable.setEditable(true);

        // Cấu hình bảng User
        // Dùng Lambda để báo lỗi viết sai tên hàm
        colUserId.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getId()));
        colUsername.setCellValueFactory(data -> new SimpleStringProperty(data.getValue().getUsername()));
        colUserSelect.setCellValueFactory(cellData -> cellData.getValue().selectedProperty());
        colUserSelect.setCellFactory(CheckBoxTableCell.forTableColumn(colUserSelect));
        userTable.setEditable(true);

        // Gắn list vào bảng
        itemTable.setItems(masterDataItems);
        userTable.setItems(masterDataUsers);

        setupSearchFilters();
        loadDataFromServer();

        itemTable.sceneProperty().addListener((obs, oldScene, newScene) -> {
            if (newScene != null) {
                newScene.windowProperty().addListener((obs2, oldWin, newWin) -> {
                    if (newWin != null) {
                        newWin.showingProperty().addListener((obs3, wasShowing, isShowing) -> {
                            if (isShowing) loadDataFromServer();
                        });
                    }
                });
            }
        });

        // Check dashboard update để tự reload khi seller đổi tên
        ClientSocket.getInstance().setDashboardUpdateListener(update -> {
            if (update == null || update.getItem() == null) return;
            Platform.runLater(() -> {
                ItemDTO updated = update.getItem();
                for (int i = 0; i < masterDataItems.size(); i++) {
                    if (Objects.equals(masterDataItems.get(i).getId(), updated.getId())) {
                        masterDataItems.set(i, updated);
                        break;
                    }
                }
            });
        });
    }

    private record LoadResult(List<ItemDTO> products, List<UserSessionDTO> users) {}

    private void loadDataFromServer() {
        itemTable.setPlaceholder(new Label("Đang tải dữ liệu..."));
        userTable.setPlaceholder(new Label("Đang tải dữ liệu..."));

        Task<LoadResult> task = new Task<>() {
            @Override
            protected LoadResult call() throws Exception {
                List<ItemDTO> products = productService.getAllProducts();
                List<UserSessionDTO> users = userService.getAllUsers();
                return new LoadResult(products, users);
            }
        };

        task.setOnSucceeded(e -> {
            LoadResult r = task.getValue();
            masterDataItems.setAll(r.products());
            masterDataUsers.setAll(r.users());

            if (r.products().isEmpty())
                itemTable.setPlaceholder(new Label("Không có sản phẩm nào"));
            if (r.users().isEmpty())
                userTable.setPlaceholder(new Label("Không có người dùng nào"));
        });

        task.setOnFailed(e -> {
            itemTable.setPlaceholder(new Label("Lỗi tải dữ liệu"));
            userTable.setPlaceholder(new Label("Lỗi tải dữ liệu"));

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Lỗi");
            alert.setHeaderText(null);
            alert.setContentText("Lỗi khi tải dữ liệu: " + task.getException().getMessage());
            alert.show();
        });

        runTask(task);
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

    @FXML
    private void handleDeleteItem() {
        List<ItemDTO> selected = masterDataItems.stream()
                .filter(ItemDTO::isSelected)
                .toList();

        if (selected.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Chưa chọn sản phẩm");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng tích chọn ít nhất một sản phẩm để xóa.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn xóa " + selected.size() + " sản phẩm đã chọn?");
        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            String adminId = com.auction.client.ClientSession.getCurrentUser().getId();

            btnDeleteItem.setDisable(true);
            btnDeleteItem.setText("Đang xóa...");

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    for (ItemDTO item : selected) {
                        if (item.getSessionId() == null || item.getSessionId().isBlank()) continue;

                        com.auction.response.AdminCancelAuctionResponse res =
                                com.auction.client.network.ClientSocket.getInstance()
                                        .sendRequestAndWait(
                                                new com.auction.request.AdminCancelAuctionRequest(adminId, item.getSessionId()),
                                                com.auction.response.AdminCancelAuctionResponse.class
                                        );

                        if (!res.isSuccess()) {
                            throw new Exception("Xóa thất bại sản phẩm \"" + item.getName() + "\": " + res.getMessage());
                        }
                    }
                    return null;
                }
            };

            task.setOnSucceeded(e -> Platform.runLater(() -> {
                masterDataItems.removeAll(selected);
                btnDeleteItem.setDisable(false);
                btnDeleteItem.setText("XÓA MỤC ĐÃ TÍCH");

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Thành công");
                ok.setHeaderText(null);
                ok.setContentText("Đã xóa " + selected.size() + " sản phẩm.");
                ok.show();
            }));

            task.setOnFailed(e -> Platform.runLater(() -> {
                btnDeleteItem.setDisable(false);
                btnDeleteItem.setText("XÓA MỤC ĐÃ TÍCH");

                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Lỗi xóa sản phẩm");
                err.setHeaderText(null);
                err.setContentText(task.getException().getMessage());
                err.show();
            }));

            runTask(task);
        });
    }

    @FXML
    private void handleDeleteUser() {
        List<UserSessionDTO> selected = masterDataUsers.stream()
                .filter(UserSessionDTO::isSelected)
                .toList();

        if (selected.isEmpty()) {
            Alert alert = new Alert(Alert.AlertType.WARNING);
            alert.setTitle("Chưa chọn người dùng");
            alert.setHeaderText(null);
            alert.setContentText("Vui lòng tích chọn ít nhất một người dùng để xóa.");
            alert.showAndWait();
            return;
        }

        String currentAdminId = com.auction.client.ClientSession.getCurrentUser().getId();
        boolean deletingSelf = selected.stream().anyMatch(u -> u.getId().equals(currentAdminId));
        if (deletingSelf) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Không hợp lệ");
            alert.setHeaderText(null);
            alert.setContentText("Bạn không thể xóa chính tài khoản đang đăng nhập.");
            alert.showAndWait();
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận xóa");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn xóa " + selected.size() + " người dùng đã chọn?");
        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            btnDeleteUser.setDisable(true);
            btnDeleteUser.setText("Đang xóa...");

            javafx.concurrent.Task<Void> task = new javafx.concurrent.Task<>() {
                @Override
                protected Void call() throws Exception {
                    for (UserSessionDTO user : selected) {
                        boolean success = userService.deleteUser(currentAdminId, user.getId());
                        if (!success) {
                            throw new Exception("Xóa thất bại người dùng \"" + user.getUsername() + "\"");
                        }
                    }
                    return null;
                }
            };

            task.setOnSucceeded(e -> Platform.runLater(() -> {
                masterDataUsers.removeAll(selected);
                btnDeleteUser.setDisable(false);
                btnDeleteUser.setText("XÓA USER ĐÃ TÍCH");

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Thành công");
                ok.setHeaderText(null);
                ok.setContentText("Đã xóa " + selected.size() + " người dùng.");
                ok.show();
            }));

            task.setOnFailed(e -> Platform.runLater(() -> {
                btnDeleteUser.setDisable(false);
                btnDeleteUser.setText("XÓA USER ĐÃ TÍCH");

                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Lỗi xóa người dùng");
                err.setHeaderText(null);
                err.setContentText(task.getException().getMessage());
                err.show();
            }));

            runTask(task);
        });
    }

    @FXML
    private void handleLogout() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Xác nhận đăng xuất");
        confirm.setHeaderText(null);
        confirm.setContentText("Bạn có chắc muốn đăng xuất?");
        confirm.showAndWait().ifPresent(response -> {
            if (response != ButtonType.OK) return;

            com.auction.client.ClientSession.clear();

            try {
                javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                        getClass().getResource("/views/login_view.fxml")
                );
                javafx.scene.Parent root = loader.load();
                Stage stage = (Stage) itemTable.getScene().getWindow();
                stage.setScene(new javafx.scene.Scene(root));
                stage.setTitle("Đăng nhập");
            } catch (java.io.IOException e) {
                e.printStackTrace();
                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Lỗi");
                err.setHeaderText(null);
                err.setContentText("Không thể quay về màn hình đăng nhập: " + e.getMessage());
                err.show();
            }
        });
    }

    private <T> void runTask(Task<T> task) {
        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}




