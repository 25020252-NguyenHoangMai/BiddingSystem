package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.service.AuctionRealtimeService;
import com.auction.client.service.AuctionService;
import com.auction.dto.ItemDTO;
import com.auction.dto.SellerHistoryItemDTO;
import com.auction.dto.SessionHistoryItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.response.BidUpdateResponse;
import com.auction.response.GetAuctionDetailResponse;
import com.auction.response.GetSellerHistoryResponse;
import com.auction.response.GetSessionHistoryResponse;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class SellerHistoryController {
    private UserSessionDTO currentUser;

    private static final String PROFILE_FXML = "/views/profile.fxml";
    private static final String SELLER_ITEM_CELL_FXML = "/views/seller_product_cell.fxml";


    @FXML private Button btnBack;
    @FXML private MenuButton menuItemType;
    @FXML private TextField txtSearch;
    @FXML private Button btnSearch;
    @FXML private MenuButton menuFilterStatus;
    @FXML private ListView<SellerHistoryItemDTO> listProducts;

    private static final String ALL_SESSIONS = "All Sessions";
    private static final String UPCOMING_SESSIONS = "Upcoming";
    private static final String RUNNING_SESSIONS = "In Progress";
    private static final String CLOSED_SESSIONS = "Closed";
    private static final String CANCELED_SESSIONS = "Canceled";
    private static final String ALL_TYPES = "All Types";

    private final ObservableList<SellerHistoryItemDTO> masterData =
            FXCollections.observableArrayList();
    private FilteredList<SellerHistoryItemDTO> filteredData;
    private final AuctionService auctionService = new AuctionService();
    private final List<Stage> childStages = new ArrayList<>();
    private Timeline autoRefreshTimeline;
    private final Map<String, AuctionRealtimeService> watchingServices = new ConcurrentHashMap<>();

    private static final String EVENT_USER_PROFILE_UPDATED = "USER_PROFILE_UPDATED";
    private static final String EVENT_ITEM_UPDATED_BY_SELLER = "ITEM_UPDATED_BY_SELLER";

    @FXML
    public void initialize() {
        setupStatusMenu();
        setupTypeMenu();

        filteredData = new FilteredList<>(masterData, session -> true);
        listProducts.setItems(filteredData);

        setupListView();
        startAutoRefresh();

        btnSearch.setOnAction(event -> applyFilters());
        txtSearch.setOnAction(event -> applyFilters());

        btnBack.setOnAction(event -> {
            stopWatchingAllSessions();
            navigateTo(PROFILE_FXML);
        });

    }

    private void setupStatusMenu() {
        menuFilterStatus.getItems().clear();
        menuFilterStatus.setText(ALL_SESSIONS);

        addMenuOption(menuFilterStatus, ALL_SESSIONS);
        addMenuOption(menuFilterStatus, UPCOMING_SESSIONS);
        addMenuOption(menuFilterStatus, RUNNING_SESSIONS);
        addMenuOption(menuFilterStatus, CLOSED_SESSIONS);
        addMenuOption(menuFilterStatus, CANCELED_SESSIONS);
    }

    private void setupTypeMenu() {
        menuItemType.getItems().clear();
        menuItemType.setText(ALL_TYPES);

        addMenuOption(menuItemType, ALL_TYPES);
        addMenuOption(menuItemType, "VEHICLE");
        addMenuOption(menuItemType, "ELECTRONICS");
        addMenuOption(menuItemType, "ART");
        addMenuOption(menuItemType, "OTHER");
    }

    private void addMenuOption(MenuButton menuButton, String text) {
        MenuItem item = new MenuItem(text);

        item.setOnAction(event -> {
            menuButton.setText(text);
            applyFilters();
        });

        menuButton.getItems().add(item);
    }

    private void applyFilters() {
        String statusFilter = menuFilterStatus.getText();
        String typeFilter = menuItemType.getText();

        String searchText = txtSearch.getText() == null
                ? ""
                : txtSearch.getText().toLowerCase().trim();

        filteredData.setPredicate(session -> {
            if (!ALL_SESSIONS.equals(statusFilter)) {
                String sessionStatus = safe(session.getStatus());

                if (CLOSED_SESSIONS.equals(statusFilter)) {
                    if (!"FINISHED".equalsIgnoreCase(sessionStatus) && !"PAID".equalsIgnoreCase(sessionStatus)) {
                        return false;
                    }
                }

                else {
                    String expectedStatus = switch (statusFilter) {
                        case CANCELED_SESSIONS -> "CANCELED";
                        case UPCOMING_SESSIONS -> "UPCOMING";
                        case RUNNING_SESSIONS -> "RUNNING";
                        default -> "";
                    };

                    if (!expectedStatus.isBlank() && !expectedStatus.equalsIgnoreCase(sessionStatus)) {
                        return false;
                    }
                }
            }

            if (!"All Types".equals(typeFilter)
                    && !safe(session.getProductType()).equalsIgnoreCase(typeFilter)) {
                return false;
            }

            if (searchText.isEmpty()) {
                return true;
            }

            return safe(session.getProductName()).toLowerCase().contains(searchText)
                    || safe(session.getSessionId()).toLowerCase().contains(searchText);
        });
    }

    private void setupListView() {
        listProducts.setPlaceholder(new Label("No auctioned products history found."));

        listProducts.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(SellerHistoryItemDTO item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource(SELLER_ITEM_CELL_FXML)
                    );

                    HBox root = loader.load();

                    SellerProductController controller = loader.getController();
                    controller.setData(item);
                    controller.setOnViewDetail(() -> handleViewDetail(item));

                    setGraphic(root);

                } catch (IOException e) {
                    e.printStackTrace();
                    setGraphic(new Label("Cannot load session item."));
                }
            }
        });
    }

    private void navigateTo(String fxmlPath) {
        try {
            Parent root = FXMLLoader.load(getClass().getResource(fxmlPath));

            Stage stage = (Stage) btnBack.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation", "Cannot open screen.");
        }
    }

    private void handleViewDetail(SellerHistoryItemDTO session) {
        if ("CANCELED".equalsIgnoreCase(session.getStatus())) {
            showAlert(Alert.AlertType.INFORMATION, "Notice", "The session is canceled.");
            return;
        }

        Task<GetAuctionDetailResponse> task = new Task<>() {
            @Override
            protected GetAuctionDetailResponse call() throws Exception {
                return auctionService.getAuctionDetail(session.getSessionId());
            }
        };

        task.setOnSucceeded(event -> {
            GetAuctionDetailResponse response = task.getValue();

            if (response == null || !response.isSuccess() || response.getItem() == null) {
                String message = response != null ? response.getMessage() : "Cannot load auction detail.";
                showAlert(Alert.AlertType.ERROR, "Auction Detail", message);
                return;
            }

            navigateToAuctionDetail(response.getItem());
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            showAlert(
                    Alert.AlertType.ERROR,
                    "Auction Detail",
                    ex != null ? ex.getMessage() : "Cannot load auction detail."
            );
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void navigateToAuctionDetail(ItemDTO item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auction_details.fxml"));
            Parent root = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setItemData(item);

            Stage stage = createModalStage("Auction — " + item.getName(), root);
            stage.initModality(Modality.NONE);
            childStages.add(stage);
            stage.setOnHidden(event -> childStages.remove(stage));
            stage.setMaximized(true);
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();
            showAlert(Alert.AlertType.ERROR, "Navigation", "Cannot open auction detail screen.");
        }
    }

    // Khi bấm nút Auctioned Products History tại màn hình profile sẽ gọi tới method này
    public void loadSellerHistoryFromServer(UserSessionDTO currentUser) {
        this.currentUser = currentUser;

        if (currentUser == null || currentUser.getId() == null || currentUser.getId().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Auctioned Product History", "User auctioned product not found.");
            return;
        }

        Task<GetSellerHistoryResponse> task = new Task<>() {
            @Override
            protected GetSellerHistoryResponse call() throws Exception {
                return auctionService.getSellerHistory(currentUser.getId());
            }
        };

        task.setOnSucceeded(event -> {
            GetSellerHistoryResponse response = task.getValue();

            if (response == null || !response.isSuccess()) {
                String message = response != null ? response.getMessage() : "Cannot load auctioned product history.";
                showAlert(Alert.AlertType.ERROR, "Auctioned Product History", message);
                return;
            }

            List<SellerHistoryItemDTO> sessions = response.getSessions();
            masterData.setAll(sessions != null ? sessions : List.of());
            startWatchingAllSessions();
            applyFilters();
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            showAlert(
                    Alert.AlertType.ERROR,
                    "Auctioned Product History",
                    ex != null ? ex.getMessage() : "Cannot load auctioned product history."
            );
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void startWatchingAllSessions() {
        // Gọi hàm hủy toàn bộ các listener đã đăng ký từ trước
        stopWatchingAllSessions();

        if (currentUser == null || currentUser.getId() == null || currentUser.getId().isBlank()) {
            return;
        }

        // Duyệt qua từng đối tượng nằm trong danh sách 'masterData'
        for (SellerHistoryItemDTO session : masterData) {
            // Lấy mã id của phiên đấu giá hiện tại
            String sessionId = session.getSessionId();
            /* Nếu id null hoặc chỉ chứa khoảng trắng
            -> Bỏ qua (continue) và nhảy sang phiên kế tiếp trong danh sách
             */
            if (sessionId == null || sessionId.isBlank()) continue;

            // Chỉ watch các session đang RUNNING — UPCOMING và CANCELED không cần
            String status = safe(session.getStatus());
            if (!"RUNNING".equalsIgnoreCase(status) && !"OPEN".equalsIgnoreCase(status)) continue;

            AuctionRealtimeService service = new AuctionRealtimeService(auctionService);
            service.setListener(this::handleSellerRealtimeUpdate);
            watchingServices.put(sessionId, service);

            Thread thread = new Thread(() -> {
                try {
                    service.watch(sessionId, currentUser.getId());
                } catch (Exception e) {
                    watchingServices.remove(sessionId);
                    System.err.println("[SellerHistory] watch failed for " + sessionId + ": " + e.getMessage());
                }
            });

            thread.setDaemon(true);
            thread.start();
        }
    }

    private void refreshListItem(SellerHistoryItemDTO session) {
        // Tìm kiếm vị trí của session đang muốn cập nhật bên trong danh sách 'masterData'
        int idx = masterData.indexOf(session);
        if (idx >= 0) {
            // set lại chính phiên đó vào lại đúng idx cũ của nó -> kích hoạt một sự kiện thay đổi (cập nhật những thay đổi mới của sesison)
            masterData.set(idx, session);
        }
    }

    private void stopWatchingAllSessions() {
        for (Map.Entry<String, AuctionRealtimeService> entry : watchingServices.entrySet()) {
            entry.getValue().unwatch(entry.getKey());
        }

        watchingServices.clear();
    }

    private void handleSellerRealtimeUpdate(BidUpdateResponse update) {
        // Thoát hàm nếu gói tin update null hoặc không có sessionId
        if (update == null || update.getSessionId() == null) return;

        Platform.runLater(() -> {
            String updatedSessionId = update.getSessionId();

            // Nếu seller tự đổi tên hoặc item được update → reload full DTO từ server
            if (EVENT_USER_PROFILE_UPDATED.equals(update.getMessage())
                    || EVENT_ITEM_UPDATED_BY_SELLER.equals(update.getMessage())) {
                if (currentUser != null) {
                    loadSellerHistoryFromServer(currentUser);
                }
                return;
            }

            // Biến masterData thành một dòng chảy dữ liệu để tìm kiếm
            masterData.stream()
                    // Chỉ dữ lại phần tử có sessionId trùng với id vừa nhận từ server
                    .filter(s -> updatedSessionId.equals(s.getSessionId()))
                    // Lấy phần tử đầu tiên tìm thấy (vì sessionId là duy nhất)
                    .findFirst()
                    .ifPresent(session -> {
                        // Cập nhật currentPrice
                        if (update.getCurrentPrice() != null) {
                            session.setCurrentPrice(update.getCurrentPrice());
                        }

                        // Cập nhật receivedBidAmount
                        if (isBidEvent(update)) {
                            if (currentUser != null) {
                                loadSellerHistoryFromServer(currentUser);
                            }
                            return;
                        }

                        boolean statusChanged = false;
                        // cập nhật status
                        if (update.getStatus() != null && !update.getStatus().equals(session.getStatus())) {
                            session.setStatus(update.getStatus());
                            // Báo hiệu status đã thay đổi
                            statusChanged = true;

                            // Nếu session đã đóng, không cần watch nữa (không còn biến động)
                            if (isClosedStatus(update.getStatus())) {
                                ClientSocket.getInstance().clearBidUpdateListener(updatedSessionId);
                            }
                        }

                        // Ép ListView phải vẽ lại dòng này với giá, total bids received và trạng thái mới
                        refreshListItem(session);

                        // Hệ thống chạy lại bộ lọc
                        if (statusChanged) {
                            applyFilters();
                        }

                    });
        });
    }

    private boolean isBidEvent(BidUpdateResponse update) {
        return update.getBidAmount() != null
                && update.getBidAmount() > 0
                && update.getBidderUsername() != null
                && !update.getBidderUsername().isBlank();
    }

    private boolean isClosedStatus(String status) {
        return "FINISHED".equals(status)
                || "CANCELED".equals(status)
                || "PAID".equals(status);
    }

    private void startAutoRefresh() {

        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> refreshAuctions())
        );

        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void refreshAuctions() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (SellerHistoryItemDTO session : masterData) {
            if (session == null) continue;
            String status = safe(session.getStatus());

            if ("OPEN".equalsIgnoreCase(status)
                    && session.getStartTimeMillis() > 0
                    && session.getStartTimeMillis() <= now
                    && session.getEndTimeMillis() > now) {
                session.setStatus("RUNNING");
                changed = true;
            }

            if (("OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status))
                    && session.getEndTimeMillis() > 0
                    && session.getEndTimeMillis() <= now) {
                session.setStatus("FINISHED");
                changed = true;
            }
        }

        if (changed) {
            listProducts.refresh();
            applyFilters();
        }
    }

    private Stage createModalStage(String title, Parent root) {
        Stage stage = new Stage();

        stage.setTitle(title);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root));

        return stage;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }

    private void showAlert(Alert.AlertType type, String title, String content) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }
}
