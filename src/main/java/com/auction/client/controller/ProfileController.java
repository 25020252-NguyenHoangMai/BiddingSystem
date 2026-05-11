package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.service.UserClientService;
import com.auction.client.util.ImageUtil;
import com.auction.dto.UserSessionDTO;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;
import java.nio.file.Path;

public class ProfileController {

    // ===== FXML =====
    @FXML private Label fullNameLabel;
    @FXML private Label usernameLabel;
    @FXML private Label totalBidsLabel;
    @FXML private Label itemsWonLabel;
    @FXML private Label activeListingsLabel;
    @FXML private VBox  activeListingsBox;

    // Nút Enable Seller — ẩn nếu đã là Seller
    @FXML private Button btnEnableSeller;

    @FXML private StackPane avatarPane;
    @FXML private ImageView avatarImageView;
    @FXML private Label avatarLabel;

    private final UserClientService userClientService = UserClientService.getInstance();

    // ===== INIT =====
    @FXML
    public void initialize() {
        UserSessionDTO user = ClientSession.getCurrentUser();
        if (user == null) return;

        // Thông tin cơ bản
        fullNameLabel.setText(user.getFullName() != null ? user.getFullName() : user.getUsername());
        usernameLabel.setText("@" + user.getUsername());

        // Stats mặc định
        totalBidsLabel.setText("0");
        itemsWonLabel.setText("0");
        activeListingsLabel.setText("0");

        // Ẩn/hiện nút Enable Seller và Active Listings tuỳ role
        refreshSellerUI(user);

        ImageUtil.loadAvatar(user.getId(), user.getUsername(), avatarImageView, avatarLabel);
    }

    // Cập nhật UI theo trạng thái sellerEnabled của user.
    private void refreshSellerUI(UserSessionDTO user) {
        boolean isSeller = user.isSellerEnabled();

        // Nếu đã là Seller: ẩn nút, hiện Active Listings
        btnEnableSeller.setVisible(!isSeller);
        btnEnableSeller.setManaged(!isSeller);
        activeListingsBox.setVisible(isSeller);
        activeListingsBox.setManaged(isSeller);

        // Label mô tả trạng thái role
        if (isSeller) {
            usernameLabel.setText("@" + user.getUsername() + " Seller");
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

            Task<Void> task = new Task<>() {
                @Override
                protected Void call() throws Exception {
                    userClientService.enableSeller(user.getId());
                    return null;
                }
            };

            task.setOnSucceeded(e -> {
                // Cập nhật ClientSession local ngay — không cần re-login
                user.setSellerEnabled(true);
                ClientSession.setCurrentUser(user);

                // Refresh UI
                refreshSellerUI(user);
                btnEnableSeller.setDisable(false);

                Alert ok = new Alert(Alert.AlertType.INFORMATION);
                ok.setTitle("Thành công");
                ok.setHeaderText(null);
                ok.setContentText("Tài khoản đã được nâng cấp lên Seller!\n"
                        + "Nút 'Add Product' đã xuất hiện ở màn hình chính.");
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

    // ===== ACTIONS =====
    @FXML
    private void handleEditProfile() {
        UserSessionDTO user = ClientSession.getCurrentUser();
        if (user == null) return;

        FileChooser chooser = new FileChooser();
        chooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg"));
        File selected = chooser.showOpenDialog(avatarPane.getScene().getWindow());

        if (selected != null) {
            // Lưu xong thì gọi lại loadAvatar để cập nhật giao diện
            ImageUtil.saveAvatarAsync(user.getId(), selected, path -> {
                ImageUtil.loadAvatar(user.getId(), user.getUsername(), avatarImageView, avatarLabel);
            });
        }
    }

    @FXML
    private void handleTransactionHistory() {}

    @FXML
    private void handleLogout() {
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
}
