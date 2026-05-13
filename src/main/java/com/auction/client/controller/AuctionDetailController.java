package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.network.ClientSocket;
import com.auction.client.service.AuctionService;
import com.auction.dto.ItemDTO;
import com.auction.request.UnwatchSessionRequest;
import com.auction.request.WatchSessionRequest;
import com.auction.response.BidUpdateResponse;
import com.auction.response.PlaceBidResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
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

public class AuctionDetailController implements ClientSocket.BidUpdateListener {

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
    private final ClientSocket socket = ClientSocket.getInstance();
    private Timeline countdownTimeline;

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
        updateBidHint(item.getCurrentPrice());

        // ===== OBSERVER: đăng ký listener + gửi WatchSessionRequest =====
        socket.setBidUpdateListener(this);
        sendWatchRequest(item.getSessionId());
    }

    // ===== OBSERVER: WATCH / UNWATCH =====
    private void sendWatchRequest(String sessionId) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() throws Exception {
                socket.connect();

                socket.sendRequest(new WatchSessionRequest(sessionId));

                // Nhận current state
                Object raw = socket.receiveResponse();

                if (raw instanceof BidUpdateResponse update) {
                    Platform.runLater(() -> {
                        currentItem.setCurrentPrice(update.getCurrentPrice());
                        currentItem.setCurrentWinnerUsername(update.getCurrentWinnerUsername());
                        currentItem.setSessionStatus(update.getStatus());

                        refreshBidState(update.getCurrentPrice(), update.getCurrentWinnerUsername(), update.getStatus());

                        updateBidHint(update.getCurrentPrice());

                        if (update.getEndTimeMillis() != null) {
                            currentItem.setEndTimeMillis(update.getEndTimeMillis());

                            startCountdown(update.getEndTimeMillis());
                        }
                    });
                }
                return null;
            }
        };

        task.setOnFailed(e ->
                System.out.println("[AuctionDetail] Watch failed: " + task.getException().getMessage()));

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private void sendUnwatchRequest(String sessionId) {
        Task<Void> task = new Task<>() {
            @Override
            protected Void call() {
                try {
                    socket.sendRequest(new UnwatchSessionRequest(sessionId));
                    // đọc response trả về
                    socket.receiveResponse();
                } catch (Exception e) {
                    System.err.println("[AuctionDetail] Unwatch failed: " + e.getMessage());
                    e.printStackTrace();
                }
                return null;
            }
        };

        Thread t = new Thread(task);

        t.setDaemon(true);

        t.start();
    }

    // ===== OBSERVER CALLBACK — gọi bởi ClientSocket reader thread =====
    @Override
    public void onBidUpdate(BidUpdateResponse update) {
        if (update == null || currentItem == null) return;
        // Lọc đúng session đang xem
        if (!Objects.equals(update.getSessionId(), currentItem.getSessionId())) return;

        Platform.runLater(() -> {
            // Cập nhật state local
            currentItem.setCurrentPrice(update.getCurrentPrice());
            currentItem.setCurrentWinnerUsername(update.getCurrentWinnerUsername());
            currentItem.setSessionStatus(update.getStatus());

            boolean isClosed = isClosedStatus(update.getStatus());

            if (update.getEndTimeMillis() != null) {
                currentItem.setEndTimeMillis(update.getEndTimeMillis());
                if (!isClosed) {
                    startCountdown(update.getEndTimeMillis());
                }
            }

            // Cập nhật UI
            refreshBidState(update.getCurrentPrice(),
                    update.getCurrentWinnerUsername(),
                    update.getStatus());
            updateBidHint(update.getCurrentPrice());
            addBidHistoryEntry(update);
        });
    }

    private void addBidHistoryEntry(BidUpdateResponse update) {
        if (update.getCurrentWinnerUsername() == null || update.getCurrentWinnerUsername().isBlank()) {
            return;
        }

        String me = ClientSession.getCurrentUser() != null
                ? ClientSession.getCurrentUser().getUsername()
                : "";

        boolean isMe = Objects.equals(update.getCurrentWinnerUsername(), me);

        String timeStr = new java.text.SimpleDateFormat("HH:mm:ss")
                .format(new java.util.Date());

        String entry = String.format("[%s]  %-18s  %s",
                timeStr,
                isMe ? update.getCurrentWinnerUsername() + " (you)" : update.getCurrentWinnerUsername(),
                fmt.format(update.getCurrentPrice()));

        lvBidHistory.getItems().add(0, entry);
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
        boolean isClosed = isClosedStatus(status);

        // Chỉ Enable nút đặt giá nếu (Không phải Seller) VÀ (Phiên chưa đóng)
        var currentUser = ClientSession.getCurrentUser();
        boolean isSeller = (currentUser != null && Objects.equals(currentUser.getId(), currentItem.getSellerId()));

        if (!isSeller) {
            btnPlaceBid.setDisable(isClosed);
        }

        if (isClosed) {
            lblTimer.setText("CLOSED");
            btnPlaceBid.setDisable(true);
            if (countdownTimeline != null) countdownTimeline.stop();
            // Huỷ listener — phiên đã đóng, không cần nhận thêm
            socket.clearBidUpdateListener();
        }
    }

    private boolean isClosedStatus(String status) {
        return "FINISHED".equals(status)
                || "CANCELED".equals(status)
                || "PAID".equals(status);
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
                currentItem.setCurrentPrice(res.getCurrentPrice());
                currentItem.setCurrentWinnerUsername(res.getCurrentWinnerUsername());
                currentItem.setSessionStatus(res.getStatus());

                refreshBidState(res.getCurrentPrice(), res.getCurrentWinnerUsername(), res.getStatus());
                updateBidHint(res.getCurrentPrice());
                updateBalanceLabel();
                txtBidAmount.clear();
                showAlert(Alert.AlertType.INFORMATION, "Success", "Bid successful!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Fail", res.getMessage());
            }
        });

        // 3. Xử lý khi lỗi (Tự động chạy trên UI Thread)
        bidTask.setOnFailed(e -> {
            resetBidButton();
            showAlert(Alert.AlertType.ERROR, "Connection Error", bidTask.getException().getMessage());
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
        // Dọn dẹp: dừng countdown, huỷ listener, gửi unwatch
        if (countdownTimeline != null) countdownTimeline.stop();
        socket.clearBidUpdateListener();
        if (currentItem != null) sendUnwatchRequest(currentItem.getSessionId());
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