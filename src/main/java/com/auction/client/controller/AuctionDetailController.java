package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.service.*;
import com.auction.client.util.BidHistoryFormatter;
import com.auction.dto.BidHistoryEntryDTO;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.response.BidUpdateResponse;
import com.auction.response.GetBidHistoryResponse;
import com.auction.response.PlaceBidResponse;
import com.auction.response.SetAutoBidResponse;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.XYChart;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

public class AuctionDetailController implements AuctionRealtimeService.AuctionUpdateListener {

    @FXML private Label lblCategory, lblName, lblTimer, lblCurrentBid, lblLeadingUser,
            lblMinBidHint, lblSeller, lblStartingPrice, lblAvailableBalance;
    @FXML private Text txtDescription;
    @FXML private GridPane gridSpecs;
    @FXML private TextField txtBidAmount;
    @FXML private ListView<String> lvBidHistory;
    @FXML private Button btnBack, btnPlaceBid, btnAutoBid;
    @FXML private LineChart<String, Number> bidPriceChart;

    // ===== STATE =====
    private ItemDTO currentItem;
    private final AuctionService auctionService = new AuctionService();
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.US);
    private Timeline countdownTimeline;
    private volatile boolean watching = true;
    private XYChart.Series<String, Number> priceSeries;
    private static final DateTimeFormatter TIME_FMT = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final ClientSessionService sessionManager = new ClientSessionService();
    private final AuctionRealtimeService realtimeManager = new AuctionRealtimeService(auctionService);


    // ===== INIT =====
    @FXML
    public void initialize() {
        lvBidHistory.setPlaceholder(new Label("No bids yet. Be the first!"));
        updateBalanceLabel();
        realtimeManager.setListener(this);

        // Khởi tạo LineChart
        priceSeries = new XYChart.Series<>();
        priceSeries.setName("Bid Price");
        bidPriceChart.getData().add(priceSeries);
        bidPriceChart.setLegendVisible(false);
        bidPriceChart.setCreateSymbols(true);
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
        //updateBidHint(item.getMinimumNextBid());

        // 6. Load lịch sử bid cũ từ server
        Task<GetBidHistoryResponse> historyTask = new Task<>() {
            @Override
            protected GetBidHistoryResponse call() throws Exception {
                return auctionService.getBidHistory(item.getSessionId());
            }
        };

        historyTask.setOnSucceeded(e -> {
            GetBidHistoryResponse res = historyTask.getValue();
            if (res != null && res.isSuccess() && res.getHistory() != null) {
                Platform.runLater(() -> {
                    lvBidHistory.getItems().clear();
                    String me = ClientSession.getCurrentUser() != null
                            ? ClientSession.getCurrentUser().getUsername() : "";

                    List<BidHistoryEntryDTO> history = new ArrayList<>(res.getHistory());
                    // Thêm vào chart theo thứ tự thời gian (cũ → mới), tối đa 20 điểm gần nhất
                    List<BidHistoryEntryDTO> chartData = history.size() > 20
                            ? history.subList(history.size() - 20, history.size())
                            : history;
                    for (BidHistoryEntryDTO entry : chartData) {
                        String timeLabel = LocalTime.ofInstant(
                                java.time.Instant.ofEpochMilli(entry.getBidTimeMillis()),
                                java.time.ZoneId.systemDefault()
                        ).format(TIME_FMT);
                        priceSeries.getData().add(
                                new XYChart.Data<>(timeLabel, entry.getBidAmount()));
                    }

                    Collections.reverse(history);
                    for (BidHistoryEntryDTO entry : history) {
                        lvBidHistory.getItems().add(BidHistoryFormatter.format(entry, me));
                    }
                });
            }
        });

        historyTask.setOnFailed(e -> {
            System.err.println("[AuctionDetail] Failed to load bid history: "
                    + historyTask.getException().getMessage());
        });

        Thread historyThread = new Thread(historyTask);
        historyThread.setDaemon(true);
        historyThread.start();

        Task<Void> watchTask = new Task<>() {
            @Override
            protected Void call() throws Exception {
                String userId = ClientSession.getCurrentUser() != null
                                ? ClientSession.getCurrentUser().getId()
                                : null;

                realtimeManager.watch(item.getSessionId(), userId);
                return null;
            }
        };

        watchTask.setOnFailed(event -> {
            Throwable ex = watchTask.getException();
            ex.printStackTrace();
            showAlert(
                    Alert.AlertType.ERROR,
                    "Watch Error",
                    "Failed to watch auction: " + ex.getMessage()
            );
        });

        Thread thread = new Thread(watchTask);
        thread.setDaemon(true);
        thread.start();
    }

    // ===== OBSERVER CALLBACK — gọi bởi ClientSocket reader thread =====
    @Override
    public void onAuctionUpdated(BidUpdateResponse update) {
        if (!watching) return;

        if (update == null || currentItem == null) return;
        // Lọc đúng session đang xem
        if (!Objects.equals(update.getSessionId(), currentItem.getSessionId())) return;

        Platform.runLater(() -> {
            // Cập nhật state local
            currentItem.setCurrentPrice(update.getCurrentPrice());
            currentItem.setCurrentWinnerUsername(update.getCurrentWinnerUsername());
            currentItem.setSessionStatus(update.getStatus());
            currentItem.setMinimumNextBid(update.getMinimumNextBid());

            boolean isClosed = isClosedStatus(update.getStatus());

            if (isClosed) {
                sessionManager.refreshCurrentUserIfAffected(currentItem, update, this::updateBalanceLabel, Throwable::printStackTrace);
            }

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
            updateBidHint(update.getMinimumNextBid());
            addBidHistoryEntry(update);
            updateBalanceLabel();
        });
    }


    private void addBidHistoryEntry(BidUpdateResponse update) {
        if (update.getBidderUsername() == null || update.getBidderUsername().isBlank()) {
            return;
        }

        String me = ClientSession.getCurrentUser() != null
                        ? ClientSession.getCurrentUser().getUsername()
                        : "";

        String entry = BidHistoryFormatter.formatRealtime(
                        update.getBidderUsername(),
                        update.getCurrentPrice(),
                        me
                );

        lvBidHistory.getItems().add(0, entry);

        // Cập nhật LineChart realtime
        String timeLabel = LocalTime.now().format(TIME_FMT);
        priceSeries.getData().add(new XYChart.Data<>(timeLabel, update.getCurrentPrice()));
        // Giữ tối đa 20 điểm gần nhất trên trục X
        if (priceSeries.getData().size() > 20) {
            priceSeries.getData().remove(0);
        }
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

            btnAutoBid.setDisable(true);

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
            btnAutoBid.setDisable(true);
            if (countdownTimeline != null) countdownTimeline.stop();
        }
    }

    private boolean isClosedStatus(String status) {
        return "FINISHED".equals(status)
                || "CANCELED".equals(status)
                || "PAID".equals(status);
    }


    private void updateBidHint(Double minimumNextBid) {
        // Nếu là seller thì không cần hiện hint
        var currentUser = ClientSession.getCurrentUser();

        if (currentUser == null
                || Objects.equals(currentUser.getId(), currentItem.getSellerId())
                || minimumNextBid == null) {

            lblMinBidHint.setText("");
            return;
        }

        lblMinBidHint.setText(String.format("Min next bid: %s", fmt.format(minimumNextBid)));
    }

    private void updateBalanceLabel() {
        UserSessionDTO user = ClientSession.getCurrentUser();

        if (user == null) { return; }

        double available = user.getAvailableBalance();

        lblAvailableBalance.setText(String.format("Available Balance: %,.0f", available));
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
                btnAutoBid.setDisable(true);
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
                if (res.getUpdatedUser() != null) {
                    ClientSession.setCurrentUser(res.getUpdatedUser());
                }

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

    @FXML
    private void handleAutoBid(ActionEvent event) {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Auto Bid");
        dialog.setHeaderText("Enable Auto Bid");
        dialog.setContentText("Enter maximum auto bid:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) { return; }

        try {
            double maxBid = Double.parseDouble(result.get());

            var currentUser = ClientSession.getCurrentUser();
            if (currentUser == null) {
                showAlert(Alert.AlertType.ERROR, "AutoBid", "User session not found.");
                return;
            }

            btnAutoBid.setDisable(true);
            btnAutoBid.setText("Processing...");

            Task<SetAutoBidResponse> task = new Task<>() {
                @Override
                protected SetAutoBidResponse call() throws Exception {
                    return auctionService.setAutoBid(
                            currentItem.getSessionId(),
                            currentUser.getId(),
                            maxBid
                    );
                }
            };

            task.setOnSucceeded(e -> {
                btnAutoBid.setDisable(false);
                btnAutoBid.setText("Auto Bid");

                SetAutoBidResponse res = task.getValue();

                if (res.isSuccess()) {
                    showAlert(Alert.AlertType.INFORMATION, "AutoBid", "Auto bid enabled successfully.");

                } else {
                    showAlert(Alert.AlertType.ERROR, "AutoBid", res.getMessage());
                }
            });

            task.setOnFailed(e -> {
                btnAutoBid.setDisable(false);
                btnAutoBid.setText("Auto Bid");
                showAlert(Alert.AlertType.ERROR, "AutoBid", task.getException().getMessage());
            });

            Thread thread = new Thread(task);
            thread.setDaemon(true);
            thread.start();

        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number.");
        }
    }

    private void resetBidButton() {
        boolean isClosed = currentItem != null && isClosedStatus(currentItem.getSessionStatus());

        var currentUser = ClientSession.getCurrentUser();

        boolean isSeller = currentUser != null && Objects.equals(currentUser.getId(), currentItem.getSellerId());

        btnPlaceBid.setDisable(isClosed || isSeller);
        btnPlaceBid.setText("Place Bid");
    }

    @FXML
    private void handleBack(ActionEvent event) {
        watching = false;

        if (currentItem != null) {
            realtimeManager.unwatch(currentItem.getSessionId());
        }

        auctionService.closeWatchSocket();

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