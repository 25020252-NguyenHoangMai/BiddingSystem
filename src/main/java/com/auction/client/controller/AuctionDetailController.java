package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.service.*;
import com.auction.client.util.BidHistoryFormatter;
import com.auction.dto.BidHistoryEntryDTO;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.response.*;
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
import java.time.Instant;
import java.time.LocalTime;
import java.time.ZoneId;
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
    private volatile boolean watching = false;
    private XYChart.Series<String, Number> priceSeries;
    private volatile boolean historyLoaded = false;
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

        bidPriceChart.setAnimated(false);

        // Tắt animation axis
        bidPriceChart.getXAxis().setAnimated(false);
        bidPriceChart.getYAxis().setAnimated(false);

        bidPriceChart.setCreateSymbols(false);

        Platform.runLater(() -> {
            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setOnCloseRequest(event -> cleanupWatching());
        });
    }

    // Điền toàn bộ dữ liệu từ ItemDTO lên màn hình
    public void setItemData(ItemDTO item) {
        if (item == null) return;
        this.currentItem = item;
        historyLoaded = false;

        populateBasicInfo(item);

        // Hiển thị thông số kỹ thuật
        setupDynamicSpecs(item);

        // Chặn Seller tự đấu giá mặt hàng của bản thân
        checkSellerPrivileges(item);

        // Đếm ngược (Sử dụng thời gian thực từ Server)
        startCountdown(item.getEndTimeMillis());

        startHistoryLoading(item);

        startRealtimeWatching(item);
    }

    private void populateBasicInfo(ItemDTO item) {
        lblCategory.setText(item.getItemType());
        lblName.setText(item.getName());
        txtDescription.setText(item.getDescription());
        lblSeller.setText(
                item.getSellerUsername() != null
                        ? item.getSellerUsername()
                        : "Unknown"
        );

        lblStartingPrice.setText(fmt.format(item.getStartingPrice()));

        refreshBidState(
                item.getCurrentPrice(),
                item.getCurrentWinnerUsername(),
                item.getSessionStatus()
        );
    }

    private void startHistoryLoading(ItemDTO item) {
        Task<GetBidHistoryResponse> historyTask = new Task<>() {
            @Override
            protected GetBidHistoryResponse call() throws Exception {
                return auctionService.getBidHistory(item.getSessionId());
            }
        };

        historyTask.setOnSucceeded(e -> {
            GetBidHistoryResponse res = historyTask.getValue();

            if (!watching || historyLoaded) return;

            if (res == null
                    || !res.isSuccess()
                    || res.getHistory() == null) {
                return;
            }

            historyLoaded = true;

            lvBidHistory.getItems().clear();
            priceSeries.getData().clear();

            String me = ClientSession.getCurrentUser() != null
                    ? ClientSession.getCurrentUser().getUsername()
                    : "";

            List<BidHistoryEntryDTO> sortedAsc = new ArrayList<>(res.getHistory());

            sortedAsc.sort(
                    Comparator.comparingLong(
                            BidHistoryEntryDTO::getBidTimeMillis
                    )
            );

            List<BidHistoryEntryDTO> chartData =
                    sortedAsc.size() > 20
                            ? sortedAsc.subList(sortedAsc.size() - 20, sortedAsc.size())
                            : sortedAsc;

            for (BidHistoryEntryDTO entry : chartData) {
                String timeLabel = LocalTime.ofInstant(
                        Instant.ofEpochMilli(entry.getBidTimeMillis()),
                        ZoneId.systemDefault()
                ).format(TIME_FMT);

                boolean exists = priceSeries.getData().stream()
                        .anyMatch(d ->
                                Objects.equals(d.getXValue(), timeLabel)
                                        && Objects.equals(d.getYValue(), entry.getBidAmount())
                        );

                if (!exists) {
                    priceSeries.getData().add(new XYChart.Data<>(timeLabel, entry.getBidAmount()));
                }
            }

            List<BidHistoryEntryDTO> sortedDesc = new ArrayList<>(sortedAsc);

            Collections.reverse(sortedDesc);

            List<BidHistoryEntryDTO> limited =
                    sortedDesc.size() > 30
                            ? sortedDesc.subList(0, 30)
                            : sortedDesc;

            for (BidHistoryEntryDTO entry : limited) {
                lvBidHistory.getItems().add(
                        BidHistoryFormatter.format(entry, me)
                );
            }
        });

        historyTask.setOnFailed(e -> {
            System.err.println(
                    "[AuctionDetail] Failed to load bid history: "
                            + historyTask.getException().getMessage()
            );
        });

        Thread historyThread = new Thread(historyTask);
        historyThread.setDaemon(true);
        historyThread.start();
    }

    private void startRealtimeWatching(ItemDTO item) {
        if (watching) { return; }

        if (item.getSessionId() == null || item.getSessionId().isBlank()) {
            // Sản phẩm chưa có phiên đấu giá — disable bid, hiện thông báo rõ ràng
            btnPlaceBid.setDisable(true);
            btnAutoBid.setDisable(true);
            lblMinBidHint.setText("Phiên đấu giá chưa bắt đầu");
            lblTimer.setText("N/A");
            return;
        }

        watching = true;

        Task<Void> watchTask = new Task<>() {
            @Override
            protected Void call() {
                try {
                    String userId =
                            ClientSession.getCurrentUser() != null
                                    ? ClientSession.getCurrentUser().getId()
                                    : null;

                    SessionWatchResponse watchResponse =
                            realtimeManager.watch(item.getSessionId(), userId);

                    Platform.runLater(() -> {
                        currentItem.setCurrentPrice(watchResponse.getCurrentPrice());
                        currentItem.setCurrentWinnerUsername(watchResponse.getCurrentWinnerUsername());
                        currentItem.setSessionStatus(watchResponse.getStatus());

                        refreshBidState(
                                watchResponse.getCurrentPrice(),
                                watchResponse.getCurrentWinnerUsername(),
                                watchResponse.getStatus()
                        );

                        updateBidHint(watchResponse.getMinimumNextBid());

                        if (watchResponse.getEndTimeMillis() != null) {
                            startCountdown(watchResponse.getEndTimeMillis());
                        }
                    });

                } catch (Exception e) {
                    e.printStackTrace();

                    Platform.runLater(() -> {
                        cleanupWatching();
                        showAlert(Alert.AlertType.ERROR, "Connection Error", "Cannot connect to realtime auction server.");
                    });
                }

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

        Thread watchThread = new Thread(watchTask);
        watchThread.setDaemon(true);
        watchThread.start();
    }

    // ===== OBSERVER CALLBACK — gọi bởi ClientSocket reader thread =====
    @Override
    public void onAuctionUpdated(BidUpdateResponse update) {
        if (!watching) return;

        if (update == null || currentItem == null) return;
        // Lọc đúng session đang xem
        if (!Objects.equals(update.getSessionId(), currentItem.getSessionId())) return;

        if ("ITEM_UPDATED_BY_SELLER".equals(update.getMessage())) {
            reloadAuctionDetail(update.getSessionId());
            return;
        }

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
            updateRealtimeChart(update);
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

        if (!lvBidHistory.getItems().contains(entry)) {
            lvBidHistory.getItems().add(0, entry);
            if (lvBidHistory.getItems().size() > 20) {
                lvBidHistory.getItems().remove(lvBidHistory.getItems().size() - 1);
            }
        }
    }

    private void updateRealtimeChart(BidUpdateResponse update) {
        String timeLabel = LocalTime.now().format(TIME_FMT);

        boolean exists = priceSeries.getData().stream()
                .anyMatch(d ->
                        Objects.equals(d.getYValue(), update.getCurrentPrice())
                );

        if (!exists) {
            priceSeries.getData().add(
                    new XYChart.Data<>(timeLabel, update.getCurrentPrice())
            );
        }

        if (priceSeries.getData().size() > 12) {
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

        boolean isClosed = isClosedStatus(status);

        updateBidButtonsState();

        if (isClosed) {
            lblTimer.setText("CLOSED");

            if (countdownTimeline != null) {
                countdownTimeline.stop();
            }
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
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = endTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                updateCountdownLabel(remaining);
            } else {
                handleAuctionExpired();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownLabel(long remainingMillis) {
        long hours = remainingMillis / 3_600_000;
        long minutes = (remainingMillis % 3_600_000) / 60_000;
        long seconds = (remainingMillis % 60_000) / 1_000;

        lblTimer.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void handleAuctionExpired() {
        lblTimer.setText("EXPIRED");

        updateBidButtonsState();

        watching = false;

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
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
        Double amount = validateBidAmount();

        if (amount == null) { return; }

        prepareBidButton();

        Task<PlaceBidResponse> bidTask = createPlaceBidTask(amount);

        configureBidTaskHandlers(bidTask);

        Thread thread = new Thread(bidTask);
        thread.setDaemon(true);
        thread.start();
    }

    private Double validateBidAmount() {
        String amountText = txtBidAmount.getText().trim();

        if (amountText.isEmpty()) {
            showAlert(Alert.AlertType.WARNING, "Missing input", "Please enter a bid amount.");
            return null;
        }

        double amount;

        try {
            amount = Double.parseDouble(amountText);

        } catch (NumberFormatException e) {
            showAlert(Alert.AlertType.WARNING, "Invalid input", "Bid amount must be a valid number.");
            return null;
        }

        if (amount <= 0) {
            showAlert(Alert.AlertType.WARNING, "Invalid input", "Bid amount must be greater than zero.");
            return null;
        }

        return amount;
    }

    @FXML
    private void handleAutoBid(ActionEvent event) {
        Double maxBid = requestAutoBidAmount();

        if (maxBid == null) {
            return;
        }

        UserSessionDTO currentUser = ClientSession.getCurrentUser();

        if (currentUser == null) {
            showAlert(Alert.AlertType.ERROR, "AutoBid", "User session not found.");
            return;
        }

        prepareAutoBidButton();

        Task<SetAutoBidResponse> task = createAutoBidTask(currentUser, maxBid);

        configureAutoBidHandlers(task);

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }


    private Double requestAutoBidAmount() {
        TextInputDialog dialog = new TextInputDialog();

        dialog.setTitle("Auto Bid");
        dialog.setHeaderText("Enable Auto Bid");
        dialog.setContentText("Enter maximum auto bid:");

        Optional<String> result = dialog.showAndWait();

        if (result.isEmpty()) {
            return null;
        }

        try {
            double maxBid = Double.parseDouble(result.get());

            if (maxBid <= 0) {
                throw new NumberFormatException();
            }

            return maxBid;

        } catch (NumberFormatException ex) {
            showAlert(Alert.AlertType.ERROR, "Invalid Input", "Please enter a valid number.");
            return null;
        }
    }

    private void prepareAutoBidButton() {
        btnAutoBid.setDisable(true);
        btnAutoBid.setText("Processing...");
    }

    private void resetAutoBidButton() {
        updateBidButtonsState();
        btnAutoBid.setText("Auto Bid");
    }

    private Task<SetAutoBidResponse> createAutoBidTask(UserSessionDTO currentUser, double maxBid) {
        return new Task<>() {
            @Override
            protected SetAutoBidResponse call() throws Exception {
                return auctionService.setAutoBid(
                        currentItem.getSessionId(),
                        currentUser.getId(),
                        maxBid
                );
            }
        };
    }

    private void configureAutoBidHandlers(Task<SetAutoBidResponse> task) {
        task.setOnSucceeded(e -> {
            resetAutoBidButton();

            SetAutoBidResponse res = task.getValue();

            if (res.isSuccess()) {
                // Nếu autobid kích hoạt và giá thay đổi (autobid đặt luôn 1 bid), cập nhật history + chart
                if (res.getCurrentPrice() != null && res.getCurrentPrice() > 0
                        && res.getCurrentWinnerUsername() != null) {
                    String me = ClientSession.getCurrentUser() != null
                            ? ClientSession.getCurrentUser().getUsername() : "";

                    // History
                    String entry = BidHistoryFormatter.formatRealtime(
                            res.getCurrentWinnerUsername(), res.getCurrentPrice(), me);
                    if (!lvBidHistory.getItems().contains(entry)) {
                        lvBidHistory.getItems().add(0, entry);
                        if (lvBidHistory.getItems().size() > 20) {
                            lvBidHistory.getItems().remove(lvBidHistory.getItems().size() - 1);
                        }
                    }

                    // Chart
                    String timeLabel = java.time.LocalTime.now().format(TIME_FMT);
                    boolean exists = priceSeries.getData().stream()
                            .anyMatch(d -> Objects.equals(d.getYValue(), res.getCurrentPrice()));
                    if (!exists) {
                        priceSeries.getData().add(new XYChart.Data<>(timeLabel, res.getCurrentPrice()));
                    }
                    if (priceSeries.getData().size() > 12) {
                        priceSeries.getData().remove(0);
                    }

                    // Cập nhật UI giá hiện tại
                    refreshBidState(res.getCurrentPrice(), res.getCurrentWinnerUsername(), res.getStatus());
                    updateBidHint(null);
                }

                showAlert(Alert.AlertType.INFORMATION, "AutoBid", "Auto bid enabled successfully.");
            } else {
                showAlert(Alert.AlertType.ERROR, "AutoBid", res.getMessage());
            }
        });

        task.setOnFailed(e -> {
            resetAutoBidButton();

            showAlert(Alert.AlertType.ERROR, "AutoBid", task.getException().getMessage());
        });
    }

    private void prepareBidButton() {
        btnPlaceBid.setDisable(true);
        btnPlaceBid.setText("Placing...");
    }

    private Task<PlaceBidResponse> createPlaceBidTask(double amount) {
        return new Task<>() {
            @Override
            protected PlaceBidResponse call() throws Exception {

                return auctionService.placeBid(
                        currentItem.getSessionId(),
                        ClientSession.getCurrentUser().getId(),
                        amount
                );
            }
        };
    }

    private void configureBidTaskHandlers(Task<PlaceBidResponse> bidTask) {
        bidTask.setOnSucceeded(e -> {
            resetBidButton();

            PlaceBidResponse res = bidTask.getValue();

            if (res.isSuccess()) {
                if (res.getUpdatedUser() != null) {
                    ClientSession.setCurrentUser(res.getUpdatedUser());
                }

                updateBalanceLabel();

                txtBidAmount.clear();

                // Cập nhật history + chart ngay lập tức
                if (res.getCurrentPrice() != null && res.getCurrentPrice() > 0) {
                    String me = ClientSession.getCurrentUser() != null
                            ? ClientSession.getCurrentUser().getUsername() : "";
                    String bidder = res.getCurrentWinnerUsername() != null
                            ? res.getCurrentWinnerUsername() : me;

                    // History
                    String entry = BidHistoryFormatter.formatRealtime(bidder, res.getCurrentPrice(), me);
                    if (!lvBidHistory.getItems().contains(entry)) {
                        lvBidHistory.getItems().add(0, entry);
                        if (lvBidHistory.getItems().size() > 20) {
                            lvBidHistory.getItems().remove(lvBidHistory.getItems().size() - 1);
                        }
                    }

                    // Chart
                    String timeLabel = java.time.LocalTime.now().format(TIME_FMT);
                    boolean exists = priceSeries.getData().stream()
                            .anyMatch(d -> Objects.equals(d.getYValue(), res.getCurrentPrice()));
                    if (!exists) {
                        priceSeries.getData().add(new XYChart.Data<>(timeLabel, res.getCurrentPrice()));
                    }
                    if (priceSeries.getData().size() > 12) {
                        priceSeries.getData().remove(0);
                    }
                }


                showAlert(Alert.AlertType.INFORMATION, "Success", "Bid successful!");
            } else {
                showAlert(Alert.AlertType.ERROR, "Fail", res.getMessage());
            }
        });

        bidTask.setOnFailed(e -> {
            resetBidButton();
            showAlert(Alert.AlertType.ERROR, "Connection Error", bidTask.getException().getMessage());
        });
    }

    private void resetBidButton() {
        updateBidButtonsState();
        btnPlaceBid.setText("Place Bid");
    }

    private void cleanupWatching() {
        watching = false;

        if (currentItem != null && currentItem.getSessionId() != null) {
            realtimeManager.unwatch(currentItem.getSessionId());
        }

        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
    }

    private void updateBidButtonsState() {
        if (currentItem == null) {
            return;
        }

        boolean isClosed = isClosedStatus(currentItem.getSessionStatus());

        UserSessionDTO currentUser = ClientSession.getCurrentUser();

        boolean isSeller =
                currentUser != null
                        && Objects.equals(
                        currentUser.getId(),
                        currentItem.getSellerId()
                );

        btnPlaceBid.setDisable(isClosed || isSeller);
        btnAutoBid.setDisable(isClosed || isSeller);
    }

    private void reloadAuctionDetail(String sessionId) {
        Task<GetAuctionDetailResponse> task = new Task<>() {
            @Override
            protected GetAuctionDetailResponse call() throws Exception {
                return auctionService.getAuctionDetail(sessionId);
            }
        };

        task.setOnSucceeded(event -> {
            GetAuctionDetailResponse response = task.getValue();

            if (response == null
                    || !response.isSuccess()
                    || response.getItem() == null) {
                return;
            }

            ItemDTO updated = response.getItem();

            currentItem = updated;

            populateBasicInfo(updated);

            setupDynamicSpecs(updated);

            if (updated.getEndTimeMillis() > 0) {
                startCountdown(updated.getEndTimeMillis());
            }

            updateBidHint(updated.getMinimumNextBid());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();

            System.err.println("[AuctionDetailController] reloadAuctionDetail failed");

            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleBack(ActionEvent event) {
        cleanupWatching();
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