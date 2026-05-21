package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.network.ClientSocket;
import com.auction.client.service.UserClientService;
import com.auction.client.util.ImageUtil;
import com.auction.dto.UserSessionDTO;

import com.auction.request.UnwatchDashboardRequest;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;

public class ProfileController {

    // ===== FXML =====
    @FXML private Label fullNameLabel;
    @FXML private Label usernameLabel;

    // Nút Enable Seller — ẩn nếu đã là Seller
    @FXML private Button btnEnableSeller;

    @FXML private StackPane avatarPane;
    @FXML private ImageView avatarImageView;
    @FXML private Label avatarLabel;

    private final UserClientService userClientService = UserClientService.getInstance();
    private SellerHistoryController sellerHistoryController;

    // ===== INIT =====
    @FXML
    public void initialize() {
        UserSessionDTO user = ClientSession.getCurrentUser();
        if (user == null) return;

        // Thông tin cơ bản
        fullNameLabel.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
        usernameLabel.setText("@" + user.getUsername());

        // Ẩn/hiện nút Enable Seller và Active Listings tuỳ role
        refreshSellerUI(user);

        ImageUtil.loadAvatar(user.getId(), user.getUsername(), avatarImageView, avatarLabel);
    }

    // Cập nhật UI theo trạng thái sellerEnabled của user.
    private void refreshSellerUI(UserSessionDTO user) {
        boolean isSeller = user.isSellerEnabled();

//        // Nếu đã là Seller: ẩn nút, hiện Active Listings
//        btnEnableSeller.setVisible(!isSeller);
//        btnEnableSeller.setManaged(!isSeller);

        // Label mô tả trạng thái role
        if (isSeller) {
            btnEnableSeller.setVisible(true);
            btnEnableSeller.setManaged(true);

            usernameLabel.setText("@" + user.getUsername() + " Seller");
            btnEnableSeller.setText("Auctioned Products History");
            btnEnableSeller.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; -fx-font-weight: bold; -fx-background-radius: 6; -fx-cursor: hand;");
            btnEnableSeller.setOnAction(event -> navigateToSellerHistory());
        }
    }

    // ===== ENABLE SELLER =====
    @FXML
    private void handleEnableSeller() {
        UserSessionDTO user = ClientSession.getCurrentUser();
        if (user == null) return;

        // Hỏi xác nhận trước
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Enable Seller");
        confirm.setHeaderText("Bạn có chắc muốn nâng cấp tài khoản?");
        confirm.setContentText(
                "Sau khi bật Seller:\n" +
                        "• Bạn có thể đăng sản phẩm đấu giá\n" +
                        "• Bạn vẫn có thể bid sản phẩm của người khác\n" +
                        "• Bạn KHÔNG thể bid sản phẩm của chính mình\n\n" +
                        "Hành động này không thể hoàn tác.");

        confirm.showAndWait().ifPresent(result -> {
            if (result != ButtonType.OK) return;

            // Gửi request trên background thread
            btnEnableSeller.setDisable(true);
            btnEnableSeller.setText("Đang xử lý...");

            Task<UserSessionDTO> task = new Task<>() {
                @Override
                protected UserSessionDTO call() throws Exception {
                    return userClientService.enableSeller(user.getId());
                }
            };

            task.setOnSucceeded(e -> {
                UserSessionDTO updatedUser = task.getValue();

                // Dùng state mới từ server
                ClientSession.setCurrentUser(updatedUser);

                // Refresh UI
                refreshSellerUI(updatedUser);
                btnEnableSeller.setDisable(false);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);

                ok.setTitle("Thành công");
                ok.setHeaderText(null);
                ok.setContentText("Tài khoản đã được nâng cấp lên Seller!\n"
                                + "Nút 'Add Product' đã xuất hiện ở màn hình chính.\n"
                                + "Auctioned Products History is available to see."
                );
                ok.showAndWait();
            });

            task.setOnFailed(e -> {
                btnEnableSeller.setDisable(false);
                btnEnableSeller.setText("Enable Seller");

                Alert err = new Alert(Alert.AlertType.ERROR);
                err.setTitle("Lỗi");
                err.setHeaderText(null);
                err.setContentText("Không thể bật Seller: " + task.getException().getMessage());
                err.showAndWait();
            });

            Thread t = new Thread(task);
            t.setDaemon(true);
            t.start();
        });
    }

    private void navigateToSellerHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/seller_history.fxml")
            );

            Parent root = loader.load();

            SellerHistoryController controller = loader.getController();
            if (controller != null) {
                controller.loadSellerHistoryFromServer(ClientSession.getCurrentUser());
            }

            Stage stage = (Stage) avatarPane.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Auctioned Products History");
            stage.centerOnScreen();

            stage.focusedProperty().addListener((obs, wasFocused, isNowFocused) -> {
                if (isNowFocused && controller != null) {
                    controller.refresh();
                }
            });

            stage.show();

        } catch (IOException e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Cannot open auctioned products screen.");
            alert.showAndWait();
        }
    }

    @FXML
    private void handleEditProfile() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/views/edit_profile.fxml"));
            Parent root = loader.load();

            EditProfileController controller = loader.getController();
            if (controller != null) {
                controller.initData(ClientSession.getCurrentUser());
            }

            Stage stage = (Stage) avatarPane.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Edit Profile");
            stage.centerOnScreen();
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi hệ thống", "Không thể tải màn hình chỉnh sửa tài khoản.");
        }
    }

    private void showError(String title, String content) {
        Alert a = new Alert(Alert.AlertType.ERROR);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    @FXML
    private void handleTransactionHistory() {}

    @FXML
    private void handleLogout() {
        // Dọn socket trước khi logout
        ClientSocket socket = ClientSocket.getInstance();
        socket.setDashboardUpdateListener(null);
        Task<Void> unwatch = new Task<>() {
            @Override
            protected Void call() {
                socket.sendRequest(new UnwatchDashboardRequest());
                return null;
            }
        };
        Thread t = new Thread(unwatch);
        t.setDaemon(true);
        t.start();

        ClientSession.clear();
        try {
            Stage profileStage = (Stage) fullNameLabel.getScene().getWindow();
            Stage stage = profileStage.getOwner() instanceof Stage ownerStage ? ownerStage : profileStage;

            Parent root = FXMLLoader.load(getClass().getResource("/views/login_view.fxml"));
            stage.setScene(new Scene(root));

            if (profileStage != stage) {
                profileStage.close();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    // ===== HELPERS =====
    private void showInfo(String title, String content) {
        Alert a = new Alert(Alert.AlertType.INFORMATION);
        a.setTitle(title);
        a.setHeaderText(null);
        a.setContentText(content);
        a.showAndWait();
    }

    public void handleSessionHistory() {
        try {
            FXMLLoader loader = new FXMLLoader(
                    getClass().getResource("/views/session_history.fxml")
            );

            Parent root = loader.load();

            SessionHistoryController controller = loader.getController();
            if (controller != null) {
                controller.loadSessionHistoryFromServer(ClientSession.getCurrentUser());
            }

            Stage stage = (Stage) avatarPane.getScene().getWindow();

            Scene scene = new Scene(root);
            stage.setScene(scene);
            stage.setTitle("Session History");
            stage.centerOnScreen();
            stage.show();

        } catch (IOException e) {
            e.printStackTrace();

            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Navigation Error");
            alert.setHeaderText(null);
            alert.setContentText("Cannot open session history screen.");
            alert.showAndWait();
        }
    }
}
