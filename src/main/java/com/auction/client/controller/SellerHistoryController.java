package com.auction.client.controller;

import com.auction.client.service.AuctionService;
import com.auction.dto.ItemDTO;
import com.auction.dto.SellerHistoryItemDTO;
import com.auction.dto.SessionHistoryItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.response.GetAuctionDetailResponse;
import com.auction.response.GetSellerHistoryResponse;
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



    @FXML
    public void initialize() {
        setupStatusMenu();
        setupTypeMenu();

        filteredData = new FilteredList<>(masterData, session -> true);
        listProducts.setItems(filteredData);

        setupListView();

        btnSearch.setOnAction(event -> applyFilters());
        txtSearch.setOnAction(event -> applyFilters());

        btnBack.setOnAction(event -> navigateTo(PROFILE_FXML));

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

    public void refresh() {
        if (currentUser != null) {
            loadSellerHistoryFromServer(currentUser);
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
