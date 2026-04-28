package com.auction.client.controller;

import com.auction.client.service.AuthService;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.event.ActionEvent;
import com.auction.client.util.SceneUtil;

import com.auction.client.network.ClientSocket;
import com.auction.request.RegisterRequest;
import com.auction.response.RegisterResponse;

public class RegisterController {
    @FXML private TextField fullNameTextField;
    @FXML private TextField UsernameTextField;
    @FXML private PasswordField setPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML
    private void handleRegister(ActionEvent event) {
        String fullName = fullNameTextField.getText();
        String username = UsernameTextField.getText();
        String password = setPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        if (fullName == null || fullName.isBlank()) {
            showError("Please enter full name.");
            return;
        }

        if (username == null || username.isBlank()) {
            showError("Please enter username.");
            return;
        }

        if (password == null || password.isBlank()) {
            showError("Please enter password.");
            return;
        }

        if (confirmPassword == null || confirmPassword.isBlank()) {
            showError("Please confirm password.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Passwords do not match.");
            return;
        }

        try {
            RegisterResponse result = AuthService.getInstance().register(fullName, username, password, confirmPassword);

            if (result == null) {
                showError("Server không phản hồi");
                return;
            }

            if (result.isSuccess()) {
                Alert alert = new Alert(Alert.AlertType.INFORMATION);
                alert.setTitle("Success");
                alert.setHeaderText(null);
                alert.setContentText(result.getMessage());
                alert.showAndWait();

                SceneUtil.changeScene(event, "/views/login_view.fxml", "Login");
            } else {
                showError(result.getMessage());
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Lỗi kết nối Server: " + e.getMessage());
        }
    }

    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Register Error");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.show();
    }
}
