package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.dto.ItemDTO;
import com.auction.dto.SessionHistoryItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.response.GetAuctionDetailResponse;
import com.auction.response.GetSessionHistoryResponse;
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

public class SessionHistoryController {
    private static final String PROFILE_FXML = "/views/profile.fxml";
    private static final String AUCTION_DETAIL_FXML = "/views/auction_details.fxml";
    private static final String SESSION_ITEM_CELL_FXML = "/views/session_item_cell.fxml";

    private static final String ALL_SESSIONS = "All Sessions";
    private static final String RUNNING_SESSIONS = "In Progress";
    private static final String FINISHED_SESSIONS = "Finished";
    private static final String CANCELED_SESSIONS = "Canceled";

    private static final String ALL_TYPES = "All Types";

    @FXML private Button btnBack;
    @FXML private Button btnSearch;
    @FXML private TextField txtSearch;
    @FXML private MenuButton menuFilter;
    @FXML private MenuButton menuItemType;
    @FXML private ListView<SessionHistoryItemDTO> listSessions;

    private final AuctionService auctionService = new AuctionService();

    private final ObservableList<SessionHistoryItemDTO> masterData =
            FXCollections.observableArrayList();

    private FilteredList<SessionHistoryItemDTO> filteredData;

    private final List<Stage> childStages = new ArrayList<>();

    @FXML
    public void initialize() {
        setupStatusMenu();
        setupTypeMenu();

        filteredData = new FilteredList<>(masterData, session -> true);
        listSessions.setItems(filteredData);

        setupListView();

        btnSearch.setOnAction(event -> applyFilters());
        txtSearch.setOnAction(event -> applyFilters());

        btnBack.setOnAction(event -> navigateTo(PROFILE_FXML));

    }

    private void setupStatusMenu() {
        menuFilter.getItems().clear();
        menuFilter.setText(ALL_SESSIONS);

        addMenuOption(menuFilter, ALL_SESSIONS);
        addMenuOption(menuFilter, RUNNING_SESSIONS);
        addMenuOption(menuFilter, FINISHED_SESSIONS);
        addMenuOption(menuFilter, CANCELED_SESSIONS);
    }

    private void setupTypeMenu() {
        menuItemType.getItems().clear();
        menuItemType.setText(ALL_TYPES);

        addMenuOption(menuItemType, ALL_TYPES);
        addMenuOption(menuItemType, "VEHICLE");
        addMenuOption(menuItemType, "ELECTRONICS");
        addMenuOption(menuItemType, "ART");
    }

    private void addMenuOption(MenuButton menuButton, String text) {
        MenuItem item = new MenuItem(text);

        item.setOnAction(event -> {
            menuButton.setText(text);
            applyFilters();
        });

        menuButton.getItems().add(item);
    }

    private void setupListView() {
        listSessions.setPlaceholder(new Label("No session history found."));

        listSessions.setCellFactory(listView -> new ListCell<>() {
            @Override
            protected void updateItem(SessionHistoryItemDTO item, boolean empty) {
                super.updateItem(item, empty);

                if (empty || item == null) {
                    setGraphic(null);
                    return;
                }

                try {
                    FXMLLoader loader = new FXMLLoader(
                            getClass().getResource(SESSION_ITEM_CELL_FXML)
                    );

                    HBox root = loader.load();

                    SessionItemCellController controller = loader.getController();
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

    public void loadSessionHistoryFromServer(UserSessionDTO currentUser) {

        if (currentUser == null || currentUser.getId() == null || currentUser.getId().isBlank()) {
            showAlert(Alert.AlertType.ERROR, "Session History", "User session not found.");
            return;
        }

        Task<GetSessionHistoryResponse> task = new Task<>() {
            @Override
            protected GetSessionHistoryResponse call() throws Exception {
                return auctionService.getSessionHistory(currentUser.getId());
            }
        };

        task.setOnSucceeded(event -> {
            GetSessionHistoryResponse response = task.getValue();

            if (response == null || !response.isSuccess()) {
                String message = response != null ? response.getMessage() : "Cannot load session history.";
                showAlert(Alert.AlertType.ERROR, "Session History", message);
                return;
            }

            List<SessionHistoryItemDTO> sessions = response.getSessions();
            masterData.setAll(sessions != null ? sessions : List.of());
            applyFilters();
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            showAlert(
                    Alert.AlertType.ERROR,
                    "Session History",
                    ex != null ? ex.getMessage() : "Cannot load session history."
            );
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void applyFilters() {
        String statusFilter = menuFilter.getText();
        String typeFilter = menuItemType.getText();

        String searchText = txtSearch.getText() == null
                ? ""
                : txtSearch.getText().toLowerCase().trim();

        filteredData.setPredicate(session -> {
            if (!ALL_SESSIONS.equals(statusFilter)) {
                String expectedStatus = switch (statusFilter) {
                    case RUNNING_SESSIONS -> "RUNNING";
                    case FINISHED_SESSIONS -> "FINISHED";
                    case CANCELED_SESSIONS -> "CANCELED";
                    default -> "";
                };

                if (!expectedStatus.isBlank()
                        && !expectedStatus.equalsIgnoreCase(safe(session.getStatus()))) {
                    return false;
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
                    || safe(session.getSessionId()).toLowerCase().contains(searchText)
                    || safe(session.getSellerUsername()).toLowerCase().contains(searchText)
                    || safe(session.getSellerFullName()).toLowerCase().contains(searchText);
        });
    }

    private void handleViewDetail(SessionHistoryItemDTO session) {
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
//            /* Lấy cửa sổ hiện tại để tái sử dụng thay vì mở hẳn một cửa sổ mới
//               Tìm bằng listSessions, xem listSessions ở cửa sổ nào để tái sử dụng cửa sổ đó
//             */
//            Stage stage = (Stage) listSessions.getScene().getWindow();
//
//            // Lưu lại giao diện (Scene) và tiêu đề (Title) để lát nữa có thể quay lại (nút Back)
//            Scene previousScene = stage.getScene();
//            String previousTitle = stage.getTitle();
//
//            // Tải giao diện mới từ file fxml
//            FXMLLoader loader = new FXMLLoader(getClass().getResource(AUCTION_DETAIL_FXML));
//            // Đọc file cấu hình giao diện `AUCTION_DETAIL_FXML` để dựng lên màn hình chi tiết đấu giá
//            Parent root = loader.load();
//
//            // Truyền dữ liệu sang controller
//            AuctionDetailController controller = loader.getController();
//            /* Lấy ra controller của màn hình mới,
//               truyền màn hình cũ vào (để làm nút Back),
//               truyền thông tin vật phẩm sang để hiển thị lên.
//             */
//            controller.setPreviousScene(previousScene, previousTitle);
//            controller.setItemData(item);
//
//            // Thay thế giao diện cũ bằng giao diện mới trên cùng một cửa sổ
//            stage.setScene(new Scene(root));
//            stage.setTitle("Auction Detail");
//
//            stage.setMaximized(true);
//            stage.show();

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

    private Stage createModalStage(String title, Parent root) {
        Stage stage = new Stage();

        stage.setTitle(title);
        stage.initModality(Modality.WINDOW_MODAL);
        stage.setScene(new Scene(root));

        return stage;
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