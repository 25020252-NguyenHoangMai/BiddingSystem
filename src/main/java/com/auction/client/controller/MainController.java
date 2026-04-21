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

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class MainController {

    // ===== CONSTANT =====
    private static final String ROLE_SELLER = "Seller";

    // ===== UI =====
    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Button addBtn;
    @FXML private TextField searchField;

    @FXML private TableView<ItemDTO> tableAuctions;
    @FXML private TableColumn<ItemDTO, String> colProductName;
    @FXML private TableColumn<ItemDTO, Double> colCurrentPrice;
    @FXML private TableColumn<ItemDTO, String> colSeller; // Kiểu String cho tên người bán
    @FXML private TableColumn<ItemDTO, String> colTime;

    private Timeline timer;

    // ===== DATA =====
    private final ObservableList<ItemDTO> auctionList = FXCollections.observableArrayList();

    // ===== SERVICE =====
    private final ProductService productService = ProductService.getInstance();

    // ===== INIT =====
    @FXML
    public void initialize() {
        setupTable();
        tableAuctions.setItems(auctionList);
        tableAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));

        // Cập nhật User Info
        setupUserInfo();

        // Tải dữ liệu
        loadProductsAsync();

        // Đếm ngưc thời gian
        startCountdown();
    }

    // ===== USER INFO =====
    private void setupUserInfo() {
        UserSessionDTO user = ClientSession.getCurrentUser();

        if (user == null) {
            // Không nên hiện Alert ngay trong init vì Stage có thể chưa sẵn sàng
            System.out.println("Phiên đăng nhập hết hạn!");
            return;
        }

        // Dùng đúng biến đã sửa tên
        welcomeLabel.setText("Welcome, " + safe(user.getUsername()));
        balanceLabel.setText(String.format("Balance: %.2f $", user.getBalance()));

        boolean isSeller = Objects.equals(user.getRole(), ROLE_SELLER);
        addBtn.setVisible(isSeller);
        addBtn.setManaged(isSeller);
    }

    // ===== TABLE CONFIG =====
    private void setupTable() {
        colProductName.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getName())));

        colCurrentPrice.setCellValueFactory(data -> new SimpleDoubleProperty(data.getValue().getCurrentPrice()).asObject());

        colSeller.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().getSellerUsername())));

        colTime.setCellValueFactory(data -> new SimpleStringProperty(safe(data.getValue().calculateTimeLeft())));
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
            auctionList.setAll(task.getValue());
            if (task.getValue().isEmpty()) {
                tableAuctions.setPlaceholder(new Label("Không có sản phẩm nào đang đấu giá"));
            }
        });

        task.setOnFailed(e -> showError("Lỗi tải dữ liệu: " + task.getException().getMessage()));

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // ===== ACTIONS (Khớp với onAction trong FXML) =====
    @FXML
    private void handleRefresh() {
        tableAuctions.setPlaceholder(new Label("Đang làm mới..."));
        loadProductsAsync();
    }

    @FXML
    private void handleLogout() {
        ClientSession.clear();
        // Sửa lại đường dẫn scene cho đúng với cấu trúc dự án của bạn
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

            // Dùng welcomeLabel để lấy Stage vì nó chắc chắn có trên giao diện
            Stage stage = (Stage) welcomeLabel.getScene().getWindow();
            Parent root = FXMLLoader.load(resource);
            stage.setScene(new Scene(root));
        } catch (IOException e) {
            e.printStackTrace();
            showError("Lỗi chuyển màn hình: " + e.getMessage());
        }
    }

    private String safe(String value) {
        return value != null ? value : "";
    }

    private void showError(String message) {
        Platform.runLater(() -> {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Thông báo lỗi");
            alert.setHeaderText(null);
            alert.setContentText(message);
            alert.show();
        });
    }

    private void startCountdown() {
        if (timer != null) timer.stop();

        timer = new Timeline(new KeyFrame(Duration.seconds(1), event -> {
            // Cứ mỗi giây, lệnh chạy một lần
            tableAuctions.refresh();
        }));

        timer.setCycleCount(Animation.INDEFINITE); // Chạy vô hạn
        timer.play();
    }
}