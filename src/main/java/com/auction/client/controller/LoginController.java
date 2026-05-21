package com.auction.client.controller;

import com.auction.client.service.AuthService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;

import com.auction.client.ClientSession;
import com.auction.response.LoginResponse;
import javafx.stage.Stage;

import java.util.ResourceBundle;

import java.net.URL;

public class LoginController implements Initializable {

    @FXML
    private TextField enterUsernameField;
    @FXML
    private TextField enterPasswordField;
    @FXML
    private Label loginMessageLabel;
    @FXML
    private ImageView brandingImageView;

    @Override
    public void initialize(URL url, ResourceBundle resourceBundle) {
        Image brandingImage = new Image(
                getClass().getResource("/images/welcome.png").toExternalForm()
        );
        brandingImageView.setImage(brandingImage);

        Platform.runLater(() -> enterUsernameField.requestFocus());

        loginMessageLabel.setVisible(false);
    }

    @FXML
    public void loginButtonOnAction(ActionEvent event) {
        String username = enterUsernameField.getText();
        String password = enterPasswordField.getText();

        loginMessageLabel.setVisible(false);

        if (username == null || username.isBlank()) {
            showError("Please enter username.");
            return;
        }

        if (password == null || password.isBlank()) {
            showError("Please enter password.");
            return;
        }

        try {
            LoginResponse response = AuthService.getInstance().login(username, password);

            if (response == null) {
                showError("Server không phản hồi");
                return;
            }

            if (response.isSuccess()) {
                System.out.println("Đăng nhập thành công!");

                ClientSession.setCurrentUser(response.getUser());

                String role = response.getUser().getRole();
                Stage stage = (Stage) ((javafx.scene.Node) event.getSource()).getScene().getWindow();

                if ("ADMIN".equalsIgnoreCase(role)) {
                    com.auction.client.util.SceneUtil.changeScene(event, "/views/admin_dashboard.fxml", "Admin Dashboard");
                } else {
                    com.auction.client.util.SceneUtil.changeScene(event, "/views/main.fxml", "Auction Dashboard");
                }

                stage.setMaximized(true);
            } else {
                showError(response.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi kết nối Server!");
        }
    }

    private void showError(String message) {
        loginMessageLabel.setText(message);
        loginMessageLabel.setVisible(true);
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        com.auction.client.util.SceneUtil.changeScene(event, "/views/register_view.fxml", "Register");
    }
}
