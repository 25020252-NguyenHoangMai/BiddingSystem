package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.util.ImageUtil;
import com.auction.dto.UserSessionDTO;
import com.auction.request.EditProfileRequest;
import com.auction.response.EditProfileResponse;
import com.auction.client.network.ClientSocket;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.StackPane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import java.io.File;

public class EditProfileController {

    @FXML
    private StackPane avatarPane;
    @FXML
    private ImageView avatarImageView;
    @FXML
    private TextField txtFullName, txtUsername;
    @FXML
    private PasswordField txtPassword;
    @FXML
    private Label errorLabel;
    @FXML
    private Button btnSave;

    private File selectedAvatarFile;
    private String currentUserId;
    private volatile boolean submitting;

    @FXML
    public void initialize() {

        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    public void initData(UserSessionDTO user) {
        if (user == null) return;

        this.currentUserId = user.getId();
        txtFullName.setText(user.getFullName() != null ? user.getFullName() : "");
        txtUsername.setText(user.getUsername());

        // Tải ảnh đại diện hiện tại của User lên ImageView
        ImageUtil.loadAvatar(user.getId(), user.getUsername(), avatarImageView, null);
    }

    // Xử lý chọn ảnh mới từ máy tính
    @FXML
    private void handleChangeAvatar() {
        FileChooser chooser = new FileChooser();
        chooser.setTitle("Select Profile Picture");
        chooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg")
        );

        File file = chooser.showOpenDialog(avatarPane.getScene().getWindow());
        if (file != null) {
            this.selectedAvatarFile = file;
            // Hiển thị xem trước ảnh vừa chọn trực quan trên UI
            javafx.scene.image.Image image = new javafx.scene.image.Image(file.toURI().toString());
            avatarImageView.setImage(image);
        }
    }

    // Xử lý gửi yêu cầu lưu thông tin lên Server khi nhấn nút Save
    @FXML
    private void handleSave(ActionEvent event) {
        if (submitting) return;

        EditProfileRequest request;
        try {
            request = buildEditProfileRequest();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
            return;
        }

        submitting = true;
        btnSave.setDisable(true);
        btnSave.setText("Saving...");
        errorLabel.setVisible(false);

        Task<EditProfileResponse> task = new Task<>() {
            @Override
            protected EditProfileResponse call() throws Exception {
                ClientSocket socket = ClientSocket.getInstance();

                return socket.sendRequestAndWait(request, EditProfileResponse.class);
            }
        };

        task.setOnSucceeded(e -> {
            EditProfileResponse response = task.getValue();

            if (response != null && response.isSuccess()) {
                UserSessionDTO updatedUser = response.userSessionDTO();

                if (selectedAvatarFile != null) {
                    ImageUtil.saveAvatarAsync(currentUserId, selectedAvatarFile, path -> {
                        javafx.application.Platform.runLater(() -> finishUpdate(updatedUser));
                    });
                } else {
                    finishUpdate(updatedUser);
                }
            } else {
                resetSubmitState();
                String errorMsg = (response != null) ? response.getMessage() : "Cập nhật thất bại.";
                errorLabel.setText(errorMsg);
                errorLabel.setVisible(true);
            }
        });

        task.setOnFailed(e -> {
            resetSubmitState();
            Throwable exception = task.getException();
            errorLabel.setText(exception != null ? exception.getMessage() : "Connection error!");
            errorLabel.setVisible(true);
        });

        new Thread(task).start();
    }

    private EditProfileRequest buildEditProfileRequest() throws Exception {
        // Kiểm tra xem User đã đăng nhập hay chưa dựa vào ClientSession của hệ thống
        var currentUser = ClientSession.getCurrentUser();
        if (currentUser == null) throw new Exception("Session expired. Please login again!");

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText(); // Giữ nguyên khoảng trắng nếu người dùng cố ý đặt

        if (username.isBlank()) {
            throw new Exception("Username cannot be empty!");
        }

        return new EditProfileRequest(currentUserId, fullName, username, password);
    }

    private void finishUpdate(UserSessionDTO updatedUser) {
        if (updatedUser != null) {
            ClientSession.setCurrentUser(updatedUser);
        }

        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Profile updated successfully!");
        alert.showAndWait();

        goToProfileScene();
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        goToProfileScene();
    }

    private void resetSubmitState() {
        submitting = false;
        btnSave.setDisable(false);
        btnSave.setText("Save Changes");
    }

    private void goToProfileScene() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(
                    getClass().getResource("/views/profile.fxml")
            );

            javafx.scene.Parent root = loader.load();

            Stage stage = (Stage) btnSave.getScene().getWindow();
            stage.setScene(new javafx.scene.Scene(root));
            stage.show();

        } catch (Exception e) {
            e.printStackTrace();
            errorLabel.setText("Cannot return to profile screen!");
            errorLabel.setVisible(true);
            resetSubmitState();
        }
    }
}