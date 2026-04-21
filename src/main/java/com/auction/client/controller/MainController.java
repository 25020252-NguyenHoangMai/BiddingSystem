package com.auction.client.controller;

import com.auction.client.service.ProductService;
import com.auction.client.ClientSession;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;

import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.stage.Stage;

import java.io.IOException;
import java.util.List;
import java.util.Objects;

public class MainController {

    // ===== CONSTANT =====
    private static final String ROLE_SELLER = "Seller";

    // ===== UI =====
    @FXML private Label lblUsername;
    @FXML private Label lblBalance;
    @FXML private Button btnAddProduct;

    @FXML private TableView<ItemDTO> tableAuctions;
    @FXML private TableColumn<ItemDTO, String> colProductName;
    @FXML private TableColumn<ItemDTO, Double> colCurrentPrice;

    // ===== DATA =====
    private final ObservableList<ItemDTO> auctionList = FXCollections.observableArrayList();

    // ===== SERVICE =====
    private final ProductService productService = ProductService.getInstance();

    // ===== INIT =====
    @FXML
    public void initialize() {

        setupUserInfo();
        setupTable();

        tableAuctions.setItems(auctionList);
        tableAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));

        loadProductsAsync();
    }

    // ===== USER INFO =====
    private void setupUserInfo() {
        UserSessionDTO user = ClientSession.getCurrentUser();

        if (user == null) {
            showError("Phiên đăng nhập hết hạn!");
            return;
        }

        lblUsername.setText("Chào, " + safe(user.getUsername()));
        lblBalance.setText(String.format("%.2f $", user.getBalance()));

        boolean isSeller = Objects.equals(user.getRole(), ROLE_SELLER);
        btnAddProduct.setVisible(isSeller);
        btnAddProduct.setManaged(isSeller);
    }

    // ===== TABLE CONFIG =====
    private void setupTable() {

        colProductName.setCellValueFactory(data ->
                new SimpleStringProperty(
                        safe(data.getValue().getName())
                )
        );

        colCurrentPrice.setCellValueFactory(data ->
                new SimpleDoubleProperty(
                        data.getValue().getCurrentPrice()
                ).asObject()
        );
    }

    // ===== LOAD DATA (ASYNC) =====
    private void loadProductsAsync() {

        Task<List<ItemDTO>> task = new Task<>() {
            @Override
            protected List<ItemDTO> call() {
                return productService.getAllProducts();
            }
        };

        task.setOnSucceeded(e -> {
            List<ItemDTO> items = task.getValue();

            auctionList.setAll(items);

            if (items.isEmpty()) {
                tableAuctions.setPlaceholder(new Label("Không có sản phẩm"));
            }
        });

        task.setOnFailed(e -> {
            showError("Lỗi tải dữ liệu: " + task.getException().getMessage());
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ===== ACTIONS =====
    @FXML
    private void handleRefresh() {
        tableAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));
        loadProductsAsync();
    }

    @FXML
    private void handleLogout() {
        ClientSession.clear();
        switchScene("/com/auction/client/view/login.fxml");
    }

    @FXML
    private void handleAddProduct() {
        switchScene("/com/auction/client/view/add_product.fxml");
    }

    // ===== NAVIGATION =====
    private void switchScene(String fxmlPath) {
        try {
            var resource = getClass().getResource(fxmlPath);

            if (resource == null) {
                showError("Không tìm thấy file: " + fxmlPath);
                return;
            }

            Stage stage = (Stage) lblUsername.getScene().getWindow();
            Parent root = FXMLLoader.load(resource);
            stage.setScene(new Scene(root));

        } catch (IOException e) {
            showError("Lỗi chuyển màn hình: " + e.getMessage());
        }
    }

    // ===== UTIL =====
    private String safe(String value) {
        return value != null ? value : "";
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setContentText(message);
            alert.show();
        });
    }
}