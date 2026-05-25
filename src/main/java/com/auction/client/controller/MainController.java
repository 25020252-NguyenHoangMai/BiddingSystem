package com.auction.client.controller;

import com.auction.client.network.ClientSocket;
import com.auction.client.service.AuctionService;
import com.auction.client.service.ProductService;
import com.auction.client.ClientSession;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;

import com.auction.request.UnwatchDashboardRequest;
import com.auction.request.WatchDashboardRequest;
import com.auction.response.DashboardUpdateResponse;
import com.auction.response.DashboardUpdateType;
import com.auction.response.DashboardWatchResponse;
import javafx.application.Platform;
import javafx.beans.property.SimpleDoubleProperty;
import javafx.beans.property.SimpleStringProperty;
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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Consumer;

import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.util.Duration;

public class MainController implements ClientSocket.DashboardUpdateListener {

    // ===== UI =====
    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Button addBtn;
    @FXML private TextField searchField;
    @FXML private ListView<ItemDTO> listAuctions;

    // ===== FILTER BUTTONS - Status =====
    @FXML private ToggleButton btnStatusAll;
    @FXML private ToggleButton btnStatusUpcoming;
    @FXML private ToggleButton btnStatusRunning;
    @FXML private ToggleButton btnStatusClosed;

    // ===== FILTER BUTTONS - Category =====
    @FXML private ToggleButton btnCatAll;
    @FXML private ToggleButton btnCatVehicle;
    @FXML private ToggleButton btnCatElectronics;
    @FXML private ToggleButton btnCatArt;
    @FXML private ToggleButton btnCatOther;

    // ===== FILTER STATE =====
    private String activeStatusFilter   = null;
    private String activeCategoryFilter = null;

    // ===== DATA =====
    private final ObservableList<ItemDTO> auctionList = FXCollections.observableArrayList();
    private final FilteredList<ItemDTO> filteredAuctions = new FilteredList<>(auctionList, p -> true);
    private final List<Stage> childStages = new ArrayList<>();

    // ===== SERVICE =====
    private final ProductService productService = ProductService.getInstance();
    private final ClientSocket clientSocket = ClientSocket.getInstance();
    private final AuctionService auctionService = new AuctionService();
    private final Consumer<UserSessionDTO> userChangeListener =
            user -> Platform.runLater(this::setupUserInfo);
    private Timeline autoRefreshTimeline;


    // ===== INIT =====
    @FXML
    public void initialize() {
        setupListView();
        listAuctions.setItems(filteredAuctions);
        listAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));

        setupUserInfo();
        ClientSession.addUserChangeListener(userChangeListener);
        setupSearchFilter();
        setupFilterButtons();

        clientSocket.setDashboardUpdateListener(this);
        sendWatchThenLoad();
        startAutoRefresh();
    }

    // ===== Gửi WatchDashboardRequest, chờ xác nhận, rồi mới tải sản phẩm =====
    private void sendWatchThenLoad() {
        listAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));
        Task<List<ItemDTO>> task = new Task<>() {
            @Override
            protected List<ItemDTO> call() throws Exception {
                clientSocket.connect();
                System.out.println("[MainController] Sending WatchDashboardRequest");

                try {
                    DashboardWatchResponse watchResp = clientSocket.sendRequestAndWait(
                            new WatchDashboardRequest(), DashboardWatchResponse.class);

                    boolean watched = watchResp != null && watchResp.isSuccess();
                    clientSocket.setDashboardWatching(watched);

                    System.out.println("[MainController] Watch response: " + watched);

                } catch (Exception e) {
                    clientSocket.setDashboardWatching(false);
                    System.err.println("[MainController] Watch dashboard failed: " + e.getMessage());
                    // Không throw vì vẫn load product
                }

                System.out.println("[MainController] Loading products...");

                List<ItemDTO> items = productService.getAllProducts();

                System.out.println("[MainController] Loaded " + items.size() + " products");

                return items;
            }
        };

        runBackgroundTask(
                task,
                this::handleProductsLoaded,
                this::handleProductLoadingError
        );
    }

    // ===== Callback khi server push sản phẩm mới =====
    @Override
    public void onDashboardUpdate(DashboardUpdateResponse update) {
        if (!isValidUpdate(update)) {
            return;
        }

        Platform.runLater(() -> processDashboardUpdate(update));
    }

    private void processDashboardUpdate(DashboardUpdateResponse update) {
        ItemDTO item = update.getItem();
        DashboardUpdateType type = update.getType();

        switch (type) {
            case ITEM_ADDED -> addAuctionItem(item);
            case ITEM_UPDATED -> updateAuctionItem(item);
            case ITEM_REMOVED -> removeAuctionItem(item);

            default -> {
            }
        }
    }

    private void addAuctionItem(ItemDTO item) {
        boolean exists = auctionList.stream()
                .anyMatch(i -> Objects.equals(i.getId(), item.getId()));

        if (!exists) {
            auctionList.add(0, item);
        }
    }

    private void updateAuctionItem(ItemDTO item) {
        for (int i = 0; i < auctionList.size(); i++) {

            ItemDTO current = auctionList.get(i);

            if (Objects.equals(current.getId(), item.getId())) {
                current.setName(item.getName());
                current.setDescription(item.getDescription());
                current.setItemType(item.getItemType());
                current.setCurrentPrice(item.getCurrentPrice());
                current.setCurrentWinnerUsername(item.getCurrentWinnerUsername());
                current.setSellerUsername(item.getSellerUsername());
                current.setImagePath(item.getImagePath());
                current.setSessionStatus(item.getSessionStatus());
                current.setEndTimeMillis(item.getEndTimeMillis());
                current.setStartTimeMillis(item.getStartTimeMillis());
                current.setMinimumNextBid(item.getMinimumNextBid());

                applyFilters();
                listAuctions.refresh();
                return;
            }
        }
    }

    private void removeAuctionItem(ItemDTO item) {
        if (item == null) {
            return;
        }
        auctionList.removeIf(i -> Objects.equals(i.getId(), item.getId()));
        applyFilters();
    }

    private boolean isValidUpdate(DashboardUpdateResponse update) {
        return update != null && update.getItem() != null;
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

        boolean isSeller = user.isSellerEnabled();
        addBtn.setVisible(isSeller);
        addBtn.setManaged(isSeller);
    }

    private void setupListView() {
        listAuctions.setCellFactory(lv -> new ListCell<>() {
            private AuctionItemCellController cellController;

            @Override
            protected void updateItem(ItemDTO item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    if (cellController != null) {
                        cellController.stopCountdown();
                        cellController = null;
                    }
                    return;
                }

//                try {
//                    FXMLLoader loader = new FXMLLoader(
//                            getClass().getResource("/views/auction_item_cell.fxml")
//                    );
//                    javafx.scene.layout.HBox root = loader.load();
//                    cellController = loader.getController();
//                    cellController.setData(item);
//                    cellController.setOnViewDetail(() -> openAuctionDetail(item));
//                    setGraphic(root);
//                } catch (java.io.IOException e) {
//                    e.printStackTrace();
//                    setGraphic(new Label("Cannot load item."));
//                }

                final ItemDTO currentItem = item;
                if (cellController == null) {
                    try {
                        FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auction_item_cell.fxml"));
                        HBox root = loader.load();
                        cellController = loader.getController();
                        cellController.setOnViewDetail(() -> openAuctionDetail(item));
                        setGraphic(root);
                    } catch (IOException e) {
                        e.printStackTrace();
                        setGraphic(new Label("Cannot load item."));
                    }
                }
                // Truyền đối tượng dữ liệu ItemDTO mới vào controller để cập nhật thông tin hiển thị lên UI
                cellController.setData(currentItem);
                // Cập nhật lại callback "Xem chi tiết" với đối tượng item hiện tại để đảm bảo khi nhấn nút sẽ mở đúng item mới nhất
                cellController.setOnViewDetail(() -> openAuctionDetail(currentItem));
            }
        });
    }


    // ===== LOAD DATA (ASYNC) =====
    private void loadProductsAsync() {
        listAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));

        Task<List<ItemDTO>> task = new Task<>() {

            @Override
            protected List<ItemDTO> call() {
                return productService.getAllProducts();
            }
        };

        runBackgroundTask(
                task,
                this::handleProductsLoaded,
                this::handleProductLoadingError
        );
    }

    private void handleProductsLoaded(List<ItemDTO> items) {
        auctionList.setAll(items);

        if (items.isEmpty()) {
            listAuctions.setPlaceholder(new Label("No item"));
        }
    }

    private void handleProductLoadingError(Throwable ex) {
        ex.printStackTrace();

        listAuctions.setPlaceholder(new Label("Cannot load data"));

        showError("Error loading data: " + ex.getMessage());
    }

    // ===== ACTIONS =====
    @FXML
    private void handleLogout() {
        cleanupApplication();
        switchScene("/views/login_view.fxml");
    }

    @FXML
    private void handleAddProduct() {
        try {
            var resource = getClass().getResource("/views/add_product2.fxml");
            if (resource == null) {
                showError("Khong tim thay file: /views/add_product2.fxml");
                return;
            }

            Parent root = FXMLLoader.load(resource);

            Stage addProductStage = createModalStage("Add Product", root);

            addProductStage.setOnHidden(event -> {
                loadProductsAsync();
            });

            addProductStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Loi mo man hinh them san pham: " + e.getMessage());
        }
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

    private Stage createModalStage(String title, Parent root) {
        Stage stage = new Stage();

        stage.setTitle(title);
        stage.initOwner(welcomeLabel.getScene().getWindow());
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root));

        return stage;
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

    @FXML
    private void handleViewProfile() {
        try {
            var resource = getClass().getResource("/views/profile.fxml");
            if (resource == null) {
                showError("Cannot find file: /views/profile.fxml");
                return;
            }

            Parent root = FXMLLoader.load(resource);

            Stage profileStage = createModalStage("Profile", root);

            profileStage.setOnHidden(event -> setupUserInfo());

            profileStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Profile screen opening error: " + e.getMessage());
        }
    }

    @FXML
    private void handleDepositBalance() {
        try {
            var resource = getClass().getResource("/views/deposit.fxml");
            if (resource == null) {
                showError("Cannot find file: /views/deposit.fxml");
                return;
            }

            Parent root = FXMLLoader.load(resource);

            Stage depositStage = createModalStage("Deposit", root);
            depositStage.setResizable(false);

            // Khi đóng màn hình nạp tiền, cập nhật lại balance ở Main
            depositStage.setOnHidden(event -> setupUserInfo());

            depositStage.show();
        } catch (IOException e) {
            e.printStackTrace();
            showError("Error opening deposit screen: " + e.getMessage());
        }
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((obs, oldVal, newVal) -> applyFilters());
    }

    // ===== FILTER SETUP =====
    private void setupFilterButtons() {
        activeStatusFilter = null;
        setButtonActive(btnStatusAll,true);
        setButtonActive(btnStatusUpcoming,false);
        setButtonActive(btnStatusRunning,false);
        setButtonActive(btnStatusClosed, false);
        setButtonActive(btnCatAll,true);
        applyFilters();
    }

    @FXML
    private void handleStatusFilter(javafx.event.ActionEvent event) {
        ToggleButton clicked = (ToggleButton) event.getSource();
        if      (clicked == btnStatusAll)      activeStatusFilter = null;
        else if (clicked == btnStatusUpcoming) activeStatusFilter = "UPCOMING";
        else if (clicked == btnStatusRunning)  activeStatusFilter = "RUNNING";
        else if (clicked == btnStatusClosed)   activeStatusFilter = "CLOSED";

        setButtonActive(btnStatusAll,      activeStatusFilter == null);
        setButtonActive(btnStatusUpcoming, "UPCOMING".equals(activeStatusFilter));
        setButtonActive(btnStatusRunning,  "RUNNING".equals(activeStatusFilter));
        setButtonActive(btnStatusClosed,   "CLOSED".equals(activeStatusFilter));
        applyFilters();
    }

    @FXML
    private void handleCategoryFilter(javafx.event.ActionEvent event) {
        ToggleButton clicked = (ToggleButton) event.getSource();
        if      (clicked == btnCatAll)         activeCategoryFilter = null;
        else if (clicked == btnCatVehicle)     activeCategoryFilter = "VEHICLE";
        else if (clicked == btnCatElectronics) activeCategoryFilter = "ELECTRONICS";
        else if (clicked == btnCatArt)         activeCategoryFilter = "ART";
        else if (clicked == btnCatOther)       activeCategoryFilter = "OTHER";


        setButtonActive(btnCatAll,         activeCategoryFilter == null);
        setButtonActive(btnCatVehicle,     "VEHICLE".equals(activeCategoryFilter));
        setButtonActive(btnCatElectronics, "ELECTRONICS".equals(activeCategoryFilter));
        setButtonActive(btnCatArt,         "ART".equals(activeCategoryFilter));
        setButtonActive(btnCatOther,         "OTHER".equals(activeCategoryFilter));
        applyFilters();
    }

    private void applyFilters() {
        String keyword = searchField.getText() == null ? "" : searchField.getText().trim().toLowerCase();

        filteredAuctions.setPredicate(item -> {
            if (item == null) { return false;}
            String status = item.getSessionStatus();
            if ("CANCELED".equalsIgnoreCase(status)) {
                return false;
            }
            // Search filter
            if (!keyword.isBlank()) {
                String name   = item.getName() == null ? "" : item.getName().toLowerCase();
                String seller = item.getSellerUsername() == null ? "" : item.getSellerUsername().toLowerCase();
                if (!name.contains(keyword) && !seller.contains(keyword)) return false;

                boolean matchKeyword = name.contains(keyword) || seller.contains(keyword);
                if (!matchKeyword) {
                    return false;
                }
            }

            // Status filter
            if (activeStatusFilter != null) {
                if (status == null) return false;
                if ("UPCOMING".equalsIgnoreCase(activeStatusFilter)) {
                    if (!"OPEN".equalsIgnoreCase(status)) return false;
                }
                if ("RUNNING".equalsIgnoreCase(activeStatusFilter)) {
                    boolean running = "OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status);
                    if (!running) {
                        return false;
                    }
                }
                if ("CLOSED".equalsIgnoreCase(activeStatusFilter)) {
                    boolean running = "OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status);
                    if (running) {
                        return false;
                    }
                }
            }

            // Category filter
            if (activeCategoryFilter != null) {
                String type = item.getItemType();
                if (type == null) return false;
                if (!activeCategoryFilter.equalsIgnoreCase(type)) return false;
            }

            return true;
        });
    }

    private void setButtonActive(ToggleButton btn, boolean active) {
        btn.setSelected(active);
        if (active) {
            btn.setStyle("-fx-background-color: #2c3e50; -fx-text-fill: white; "
                    + "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 4 14;");
        } else {
            btn.setStyle("-fx-background-color: #ecf0f1; -fx-text-fill: #2c3e50; "
                    + "-fx-background-radius: 20; -fx-cursor: hand; -fx-font-size: 12px; -fx-padding: 4 14;");
        }
    }

    private void openAuctionDetail(ItemDTO item) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/auction_details.fxml"));
            Parent root = loader.load();

            AuctionDetailController controller = loader.getController();
            controller.setItemData(item);

            Stage stage = createModalStage("Auction — " + item.getName(), root);
            stage.initModality(Modality.NONE);
            childStages.add(stage);
            stage.setOnHidden(event -> {
                childStages.remove(stage);
                // Cập nhật lại item trong list với dữ liệu mới nhất từ controller
                ItemDTO updatedItem = controller.getCurrentItem();
                if (updatedItem != null) {
                    for (int i = 0; i < auctionList.size(); i++) {
                        if (Objects.equals(auctionList.get(i).getId(), updatedItem.getId())) {
                            auctionList.set(i, updatedItem);
                            break;
                        }
                    }
                }
            });
            stage.show();
        } catch (java.io.IOException e) {
            e.printStackTrace();
            showError("Cannot open auction detail: " + e.getMessage());
        }
    }

    private <T> void runBackgroundTask(
            Task<T> task,
            Consumer<T> onSuccess,
            Consumer<Throwable> onError
    ) {

        task.setOnSucceeded(event ->
                onSuccess.accept(task.getValue())
        );

        task.setOnFailed(event ->
                onError.accept(task.getException())
        );

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    // Đòng hết tất cả màn hình khi log out
    private void closeChildStages() {
        List<Stage> stagesToClose = new ArrayList<>(childStages);
        childStages.clear();

        for (Stage stage : stagesToClose) {
            if (stage != null && stage.isShowing()) {
                stage.close();
            }
        }
    }

    private void cleanupApplication() {
        closeChildStages();

        clientSocket.setDashboardUpdateListener(null);

        auctionService.closeAllSockets();

        ClientSession.removeUserChangeListener(userChangeListener);

        ClientSession.clear();
    }

    private void startAutoRefresh() {

        autoRefreshTimeline = new Timeline(
                new KeyFrame(Duration.seconds(5), e -> refreshExpiredAuctions())
        );

        autoRefreshTimeline.setCycleCount(Animation.INDEFINITE);
        autoRefreshTimeline.play();
    }

    private void refreshExpiredAuctions() {
        long now = System.currentTimeMillis();
        boolean changed = false;

        for (ItemDTO item : auctionList) {
            if (item == null) {
                continue;
            }

            String status = item.getSessionStatus();

            // OPEN -> RUNNING khi đã đến startTime
            if ("OPEN".equalsIgnoreCase(status)
                    && item.getStartTimeMillis() > 0
                    && item.getStartTimeMillis() <= now
                    && item.getEndTimeMillis() > now) {
                item.setSessionStatus("RUNNING");
                changed = true;
            }

            if (("OPEN".equalsIgnoreCase(status) || "RUNNING".equalsIgnoreCase(status)) && item.getEndTimeMillis() <= now) {
                item.setSessionStatus("FINISHED");
                changed = true;
            }
        }

        if (changed) {
            listAuctions.refresh();
            applyFilters();
        }
    }
}