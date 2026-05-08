package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.service.AuctionService;
import com.auction.dto.ItemDTO;
import com.auction.response.PlaceBidResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.Locale;
import java.util.Objects;

public class AuctionDetailController {

    @FXML private Label lblCategory, lblName, lblTimer, lblCurrentBid, lblLeadingUser,
            lblMinBidHint, lblBalance, lblSeller, lblStartingPrice;
    @FXML private Text txtDescription;
    @FXML private GridPane gridSpecs;
    @FXML private TextField txtBidAmount;
    @FXML private ListView<String> lvBidHistory;
    @FXML private Button btnBack, btnPlaceBid;

    // ===== STATE =====
    private ItemDTO currentItem;
    private final AuctionService auctionService = new AuctionService();
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.US);
    private Timeline countdownTimeline;
    private Timeline refreshTimeline;


    // ===== INIT =====
    @FXML
    public void initialize() {
        lvBidHistory.setPlaceholder(new Label("No bids yet. Be the first!"));
        updateBalanceLabel();
    }

    // Điền toàn bộ dữ liệu từ ItemDTO lên màn hình
    public void setItemData(ItemDTO item) {
        if (item == null) return;
        this.currentItem = item;

        // 1. Thông tin cơ bản
        lblCategory.setText(item.getItemType());
        lblName.setText(item.getName());
        txtDescription.setText(item.getDescription());
        lblSeller.setText(item.getSellerUsername() != null ? item.getSellerUsername() : "Unknown");
        lblStartingPrice.setText(fmt.format(item.getStartingPrice()));

        // 2. Cập nhật trạng thái bid hiện tại
        refreshBidState(item.getCurrentPrice(), item.getCurrentWinnerUsername(), item.getSessionStatus());

        // 3. Hiển thị thông số kỹ thuật động (xe, điện tử, nghệ thuật...)
        setupDynamicSpecs(item);

        // 4. Chặn Seller tự đấu giá mặt hàng của bản thân
        checkSellerPrivileges(item);

        // 5. Đếm ngược (Sử dụng thời gian thực từ Server)
        startCountdown(item.getEndTimeMillis());
        startRealtimeRefresh();
        updateBidHint(item.getCurrentPrice());
    }

    // Vô hiệu hóa tính năng đặt bid nếu là Seller
    private void checkSellerPrivileges(ItemDTO item) {
        var currentUser = ClientSession.getCurrentUser();
        // Kiểm tra ID người dùng hiện tại có trùng với SellerId của món hàng không
        if (currentUser != null && Objects.equals(currentUser.getId(), item.getSellerId())) {
            txtBidAmount.setDisable(true);
            txtBidAmount.setPromptText("You cannot bid on your own item");

            btnPlaceBid.setDisable(true);
            btnPlaceBid.setText("Management Mode");

            lblMinBidHint.setText("Monitoring auction as Seller");
        }
    }

    private void refreshBidState(double currentPrice, String winnerUsername, String status) {
        lblCurrentBid.setText(fmt.format(currentPrice));

        if (winnerUsername != null && !winnerUsername.isBlank()) {
            lblLeadingUser.setText("Leading: " + winnerUsername);
        } else {
            lblLeadingUser.setText("Leading: No bids yet");
        }

        // Kiểm tra xem phiên đấu giá đã kết thúc chưa
        boolean isClosed = "FINISHED".equals(status) || "CANCELED".equals(status) || "PAID".equals(status);

        // Chỉ Enable nút đặt giá nếu (Không phải Seller) VÀ (Phiên chưa đóng)
        var currentUser = ClientSession.getCurrentUser();
        boolean isSeller = (currentUser != null && Objects.equals(currentUser.getId(), currentItem.getSellerId()));

        if (!isSeller) {
            btnPlaceBid.setDisable(isClosed);
        }

        if (isClosed) {
            lblTimer.setText("CLOSED");
            btnPlaceBid.setDisable(true);
        }

        if (refreshTimeline != null) {
            refreshTimeline.stop();
        }
    }

    private void updateBidHint(double currentPrice) {
        double minStep = Math.max(10.0, currentPrice * 0.01);
        double minNextBid = currentPrice + minStep;

        // Nếu là seller thì không cần hiện hint về giá đặt tiếp theo
        var currentUser = ClientSession.getCurrentUser();
        if (currentUser != null && !Objects.equals(currentUser.getId(), currentItem.getSellerId())) {
            lblMinBidHint.setText(String.format("Min next bid: %s", fmt.format(minNextBid)));
        }
    }

    private void updateBalanceLabel() {
        if (ClientSession.getCurrentUser() != null) {
            lblBalance.setText(fmt.format(ClientSession.getCurrentUser().getBalance()));
        } else {
            lblBalance.setText("$0.00");
        }
    }

    private void startCountdown(long endTimeMillis) {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = endTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                long hours   = remaining / 3_600_000;
                long minutes = (remaining % 3_600_000) / 60_000;
                long seconds = (remaining % 60_000) / 1_000;
                lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));

            } else {
                lblTimer.setText("EXPIRED");
                btnPlaceBid.setDisable(true);
                countdownTimeline.stop();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    // ===== REALTIME REFRESH =====
    private void startRealtimeRefresh() {
        refreshTimeline = new Timeline(
                new KeyFrame(
                        Duration.seconds(2),
                        e -> refreshAuctionData()
                )
        );

        refreshTimeline.setCycleCount(
                Timeline.INDEFINITE
        );

        refreshTimeline.play();
    }

    private void refreshAuctionData() {
        Task<ItemDTO> task = new Task<>() {

            @Override
            protected ItemDTO call() throws Exception {
                return auctionService.refreshAuction(
                        currentItem.getSessionId()
                );
            }
        };

        task.setOnSucceeded(e -> {

            ItemDTO updated = task.getValue();
            if (updated == null) return;

            currentItem.setCurrentPrice(updated.getCurrentPrice());
            currentItem.setCurrentWinnerUsername(updated.getCurrentWinnerUsername());
            currentItem.setSessionStatus(updated.getSessionStatus());

            refreshBidState(
                    updated.getCurrentPrice(),
                    updated.getCurrentWinnerUsername(),
                    updated.getSessionStatus()
            );

            updateBidHint(updated.getCurrentPrice());
        });

        new Thread(task).start();
    }

    private void setupDynamicSpecs(ItemDTO item) {
        gridSpecs.getChildren().clear();
        int row = 0;
        String type = (item.getItemType() != null) ? item.getItemType().toUpperCase() : "";
        switch (type) {
            case "VEHICLE" -> {
                addSpecRow("Model:",   item.getModel(),      row++);
                addSpecRow("Engine:",  item.getEngineType(), row++);
                addSpecRow("Mileage:", item.getMileage() + " km", row);
            }
            case "ELECTRONICS" -> addSpecRow("Brand:",  item.getBrand(),  row);
            case "ART"         -> addSpecRow("Artist:", item.getArtist(), row);
            default -> {}
        }
    }

    private void addSpecRow(String label, String value, int row) {
        Label lbl = new Label(label);
        Label val = new Label(value != null ? value : "N/A");
        gridSpecs.add(lbl, 0, row);
        gridSpecs.add(val, 1, row);
    }

    // ===== ACTIONS =====

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        String amountText = txtBidAmount.getText().trim();
        if (amountText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing input", "Please enter a bid amount.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid input", "Bid amount must be a valid number.");
            return;
        }

        if (amount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid input", "Bid amount must be greater than zero.");
            return;
        }

        // Khóa nút để tránh spam click
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Placing...");

        Task<PlaceBidResponse> bidTask = new Task<>() {
            @Override
            protected PlaceBidResponse call() throws Exception {
                return auctionService.placeBid(
                        currentItem.getSessionId(),
                        ClientSession.getCurrentUser().getId(),
                        amount
                );
            }
        };

        // 2. Xử lý khi thành công (Tự động chạy trên UI Thread)
        bidTask.setOnSucceeded(e -> {
            resetBidButton();
            PlaceBidResponse res = bidTask.getValue();

            if (res.isSuccess()) {
                // Cập nhật UI
                refreshBidState(res.getCurrentPrice(), res.getCurrentWinnerUsername(), res.getStatus());
                updateBidHint(res.getCurrentPrice());
                updateBalanceLabel();
                txtBidAmount.clear();
                showAlert(Alert.AlertType.INFORMATION, "Thành công", "Đã đặt giá!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Thất bại", res.getMessage());
            }
        });

        // 3. Xử lý khi lỗi (Tự động chạy trên UI Thread)
        bidTask.setOnFailed(e -> {
            resetBidButton();
            showAlert(Alert.AlertType.ERROR, "Lỗi kết nối", bidTask.getException().getMessage());
        });

        // Chạy task
        new Thread(bidTask).start();
    }

    private void resetBidButton() {
        btnPlaceBid.setDisable(false);
        btnPlaceBid.setText("Place Bid");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        if (countdownTimeline != null) countdownTimeline.stop();
        if (refreshTimeline != null) refreshTimeline.stop();
        ((Stage) btnBack.getScene().getWindow()).close();
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}