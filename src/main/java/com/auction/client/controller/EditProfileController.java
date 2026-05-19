package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.util.ImageUtil;
import com.auction.dto.UserSessionDTO;
import com.auction.request.EditProfileRequest;
import com.auction.response.EditProfileResponse;
import com.auction.response.Response;
import com.auction.client.network.ClientSocket; // Đảm bảo hàm sendRequest của bạn đã được sửa thành static
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

    @FXML private StackPane avatarPane;
    @FXML private ImageView avatarImageView;
    @FXML private TextField txtFullName, txtUsername;
    @FXML private PasswordField txtPassword;
    @FXML private Label errorLabel;
    @FXML private Button btnSave;

    private File selectedAvatarFile;
    private String currentUserId;
    private volatile boolean submitting; // Quản lý trạng thái gửi request giống AddProduct2

    @FXML
    public void initialize() {
        // Khởi tạo các cấu hình ban đầu cho giao diện nếu cần (ẩn label lỗi)
        if (errorLabel != null) {
            errorLabel.setVisible(false);
        }
    }

    /**
     * Nhận dữ liệu từ ProfileController truyền sang để đổ vào các ô nhập liệu
     */
    public void initData(UserSessionDTO user) {
        if (user == null) return;

        this.currentUserId = user.getId();
        txtFullName.setText(user.getFullName() != null ? user.getFullName() : "");
        txtUsername.setText(user.getUsername());

        // Tải ảnh đại diện hiện tại của User lên ImageView
        ImageUtil.loadAvatar(user.getId(), user.getUsername(), avatarImageView, null);
    }

    /**
     * Xử lý chọn ảnh mới từ máy tính
     */
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

    /**
     * Xử lý gửi yêu cầu lưu thông tin lên Server khi nhấn nút Save
     */
    @FXML
    private void handleSave(ActionEvent event) {
        if (submitting) return;

        EditProfileRequest request;
        try {
            // Thực hiện validate dữ liệu nhập vào
            request = buildEditProfileRequest();
        } catch (Exception e) {
            errorLabel.setText(e.getMessage());
            errorLabel.setVisible(true);
            return;
        }

        // Chuyển trạng thái UI sang chế độ đang xử lý
        submitting = true;
        btnSave.setDisable(true);
        btnSave.setText("Saving...");
        errorLabel.setVisible(false);

        // Khởi tạo tác vụ ngầm (Background Task) để gọi Socket không gây block UI chính
        Task<Response> task = new Task<>() {
            @Override
            protected Response call() throws Exception {
                // Gửi gói tin thông qua ClientSocket (yêu cầu hàm sendRequest đã được chuyển sang static)
                ClientSocket socket = ClientSocket.getInstance();
                socket.sendRequest(request);
                return null;
            }
        };

        // Kịch bản giao tiếp mạng thành công (Server có phản hồi về)
        task.setOnSucceeded(e -> {
            Response generalResponse = task.getValue();

            // Ép kiểu an toàn bằng Pattern Matching (Java 14+)
            if (generalResponse instanceof EditProfileResponse response && response.isSuccess()) {
                UserSessionDTO updatedUser = response.userSessionDTO();

                // Nếu user có chọn file ảnh mới, tiến hành upload bất đồng bộ lên Server lưu trữ
                if (selectedAvatarFile != null) {
                    ImageUtil.saveAvatarAsync(currentUserId, selectedAvatarFile, path -> {
                        finishUpdate(updatedUser);
                    });
                } else {
                    finishUpdate(updatedUser);
                }
            } else {
                // Xử lý lỗi logic từ Server (ví dụ: Trùng Username, mật khẩu không hợp lệ...)
                resetSubmitState();
                String errorMsg = (generalResponse != null) ? generalResponse.getMessage() : "Update failed.";
                errorLabel.setText(errorMsg);
                errorLabel.setVisible(true);
            }
        });

        // Kịch bản lỗi kết nối vật lý (Sập Server, timeout mạng, mất kết nối Socket)
        task.setOnFailed(e -> {
            resetSubmitState();
            Throwable exception = task.getException();
            errorLabel.setText(exception != null ? exception.getMessage() : "Connection error!");
            errorLabel.setVisible(true);
        });

        // Kích hoạt Thread chạy ngầm độc lập
        new Thread(task).start();
    }

    /**
     * Rút trích và Validate dữ liệu tại Client trước khi đóng gói
     */
    private EditProfileRequest buildEditProfileRequest() throws Exception {
        // Kiểm tra xem User đã đăng nhập hay chưa dựa vào ClientSession của hệ thống
        var currentUser = ClientSession.getCurrentUser();
        if (currentUser == null) throw new Exception("Session expired. Please login again!");

        String fullName = txtFullName.getText().trim();
        String username = txtUsername.getText().trim();
        String password = txtPassword.getText(); // Giữ nguyên khoảng trắng nếu người dùng cố ý đặt

        // Validation cơ bản
        if (username.isBlank()) {
            throw new Exception("Username cannot be empty!");
        }

        // Tạo đường dẫn tạm thời cho avatar (hoặc null nếu không thay đổi)
        String avatarUrl = selectedAvatarFile != null ? selectedAvatarFile.getAbsolutePath() : null;

        // Đóng gói thành bản tin Request hoàn chỉnh
        return new EditProfileRequest(fullName, username, password, avatarUrl);
    }

    /**
     * Hoàn tất cập nhật, đồng bộ hóa bộ nhớ ClientSession và đóng View
     */
    private void finishUpdate(UserSessionDTO updatedUser) {
        if (updatedUser != null) {
            // Cập nhật lại đối tượng User toàn cục trong ứng dụng Client
            ClientSession.setCurrentUser(updatedUser);
        }

        // Hiển thị Alert thông báo thành công
        Alert alert = new Alert(Alert.AlertType.INFORMATION, "Profile updated successfully!");
        alert.showAndWait();

        close();
    }

    @FXML
    private void handleCancel(ActionEvent event) {
        close();
    }

    private void resetSubmitState() {
        submitting = false;
        btnSave.setDisable(false);
        btnSave.setText("Save Changes");
    }

    private void close() {
        ((Stage) btnSave.getScene().getWindow()).close();
    }
}