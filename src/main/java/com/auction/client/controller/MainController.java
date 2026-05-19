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
    @FXML
    private Label welcomeLabel;
    @FXML
    private Label balanceLabel;
    @FXML
    private Button addBtn;
    @FXML
    private TextField searchField;

    @FXML
    private TableView<ItemDTO> tableAuctions;
    @FXML
    private TableColumn<ItemDTO, String> colProductName;
    @FXML
    private TableColumn<ItemDTO, Double> colCurrentPrice;
    @FXML
    private TableColumn<ItemDTO, String> colSeller; // Kiểu String cho tên người bán
    @FXML
    private TableColumn<ItemDTO, String> colTime;

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


    // ===== INIT =====
    @FXML
    public void initialize() {
        setupTable();
        tableAuctions.setItems(auctionList);
        tableAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));

        // Cập nhật User Info
        setupUserInfo();

        // Đưa vào danh sách listeners
        ClientSession.addUserChangeListener(userChangeListener);

        // Chuyển màn hình Main sang màn hình đấu giá
        setupRowClickToDetail();

        setupSearchFilter();

        // Đăng ký lắng nghe realtime update từ server
        clientSocket.setDashboardUpdateListener(this);

        // Gửi WatchDashboardRequest VÀ tải danh sách sản phẩm tuần tự trên 1 thread
        // để tránh race condition: WatchDashboardResponse bị lấy nhầm bởi getAllProducts()
        if (!clientSocket.isDashboardWatching()) {
            sendWatchThenLoad();
        } else {
            loadProductsAsync();
        }
    }

    // ===== Gửi WatchDashboardRequest, chờ xác nhận, rồi mới tải sản phẩm =====
    private void sendWatchThenLoad() {
        tableAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));
        Task<List<ItemDTO>> task = new Task<>() {
            @Override
            protected List<ItemDTO> call() throws Exception {
                clientSocket.connect();
                System.out.println("[MainController] Sending WatchDashboardRequest");

                try {
                    DashboardWatchResponse watchResp = clientSocket.sendRequestAndWait(
                            new WatchDashboardRequest(), DashboardWatchResponse.class);

                    System.out.println("[MainController] Watch response: " + watchResp.isSuccess());

                } catch (Exception e) {
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

        reapplySearchFilterIfNeeded();
    }

    private void addAuctionItem(ItemDTO item) {
        boolean exists = auctionList.stream()
                .anyMatch(i -> Objects.equals(i.getId(), item.getId()));

        if (!exists) {
            auctionList.add(item);
        }
    }

    private void updateAuctionItem(ItemDTO item) {
        for (int i = 0; i < auctionList.size(); i++) {

            ItemDTO current = auctionList.get(i);

            if (Objects.equals(current.getId(), item.getId())) {
                auctionList.set(i, item);
                return;
            }
        }
    }

    private void removeAuctionItem(ItemDTO item) {
        auctionList.removeIf(i ->
                Objects.equals(i.getId(), item.getId()));
    }

    private boolean isValidUpdate(DashboardUpdateResponse update) {
        return update != null && update.getItem() != null;
    }

    private void reapplySearchFilterIfNeeded() {
        String keyword = searchField.getText();

        if (keyword != null && !keyword.isBlank()) {
            applyFilter(keyword);
        }
    }

    private void applyFilter(String keyword) {
        ObservableList<ItemDTO> filteredList = FXCollections.observableArrayList(
                auctionList.stream()
                        .filter(item -> item.getName() != null &&
                                item.getName().toLowerCase().contains(keyword.toLowerCase()))
                        .toList()
        );
        tableAuctions.setItems(filteredList);
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

    // ===== TABLE CONFIG =====
    private void setupTable() {
        setupProductNameColumn();
        setupCurrentPriceColumn();
        setupSellerColumn();
        setupTimeColumn();
    }

    private void setupProductNameColumn() {
        colProductName.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getName()))
        );
    }

    private void setupCurrentPriceColumn() {
        colCurrentPrice.setCellValueFactory(data ->
                new SimpleDoubleProperty(data.getValue().getCurrentPrice()).asObject()
        );
    }

    private void setupSellerColumn() {
        colSeller.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().getSellerUsername()))
        );
    }

    private void setupTimeColumn() {
        colTime.setCellValueFactory(data ->
                new SimpleStringProperty(safe(data.getValue().calculateTimeLeft()))
        );

        colTime.setCellFactory(column -> createTimeCell());
    }

    private TableCell<ItemDTO, String> createTimeCell() {
        return new TableCell<>() {

            private final Timeline timeline = new Timeline(
                    new KeyFrame(Duration.seconds(1), e -> updateTime()));
            {
                timeline.setCycleCount(Animation.INDEFINITE);
            }

            private void updateTime() {
                ItemDTO item = getTableRow().getItem();

                if (item != null) {
                    setText(safe(item.calculateTimeLeft()));
                }
            }

            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || getTableRow().getItem() == null) {
                    setText(null);
                    timeline.stop();
                } else {
                    updateTime();

                    if (timeline.getStatus() != Animation.Status.RUNNING) {
                        timeline.play();
                    }
                }
            }

            @Override
            public void updateIndex(int index) {
                super.updateIndex(index);

                // Cell bị recycle → stop timeline cũ
                if (index < 0) {
                    timeline.stop();
                }
            }
        };
    }

    // ===== LOAD DATA (ASYNC) =====
    private void loadProductsAsync() {
        tableAuctions.setPlaceholder(new Label("Đang tải dữ liệu..."));

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

        tableAuctions.setItems(auctionList);

        if (items.isEmpty()) {
            tableAuctions.setPlaceholder(new Label("No item"));
        }
    }

    private void handleProductLoadingError(Throwable ex) {
        ex.printStackTrace();

        tableAuctions.setPlaceholder(new Label("Cannot load data"));

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
            var resource = getClass().getResource("/views/add_product.fxml");
            if (resource == null) {
                showError("Khong tim thay file: /views/add_product.fxml");
                return;
            }

            Parent root = FXMLLoader.load(resource);

            Stage addProductStage = createModalStage("Add Product", root);

            addProductStage.setOnHidden(event -> {
                if (auctionList.isEmpty()) loadProductsAsync();
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
                showError("Khong tim thay file: /views/profile.fxml");
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
            showError("Lỗi mở màn hình nạp tiền: " + e.getMessage());
        }
    }

    private void updateAuctionTable(List<ItemDTO> items) {
        auctionList.setAll(items);
        tableAuctions.setItems(auctionList);

        if (items.isEmpty()) {
            tableAuctions.setPlaceholder(new Label("No item"));
        }
    }

    private void handleLoadProductsError(Throwable ex) {
        ex.printStackTrace();
        tableAuctions.setPlaceholder(new Label("Fail to load data"));
        showError("Cannot load data: " + ex.getMessage());
    }

    private void setupRowClickToDetail() {
        tableAuctions.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ItemDTO selected = tableAuctions.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    openAuctionDetail(selected);
                }
            }
        });
    }

    private void setupSearchFilter() {
        searchField.textProperty().addListener((obs, oldValue, newValue) -> {
            String keyword = newValue == null
                    ? ""
                    : newValue.trim().toLowerCase();

            filteredAuctions.setPredicate(item -> {
                if (keyword.isBlank()) {
                    return true;
                }

                return item.getName() != null
                        && item.getName().toLowerCase().contains(keyword);
            });
        });
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
            stage.setOnHidden(event -> childStages.remove(stage));
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
}