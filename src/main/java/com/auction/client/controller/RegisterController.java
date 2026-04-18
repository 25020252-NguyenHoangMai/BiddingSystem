package com.auction.client.controller;

import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
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
    @FXML private ChoiceBox<String> roleChoiceBox;
    @FXML private Label storeNameLabel;
    @FXML private TextField storeNameField;

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().addAll("BIDDER", "SELLER");

        roleChoiceBox.setValue("BIDDER");

        storeNameLabel.setVisible(false);
        storeNameField.setVisible(false);
        storeNameLabel.setManaged(false);
        storeNameField.setManaged(false);

        // Chỉ hiện ra Store Name khi là Seller
        roleChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSeller = "SELLER".equals(newVal);

            storeNameLabel.setVisible(isSeller);
            storeNameField.setVisible(isSeller);

            storeNameLabel.setManaged(isSeller);
            storeNameField.setManaged(isSeller);

            if (!isSeller) {
                storeNameField.clear();
            }
        });
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String fullName = fullNameTextField.getText();
        String username = UsernameTextField.getText();
        String password = setPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String role = roleChoiceBox.getValue();
        String storeName = storeNameField.getText();

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

        if (role == null || role.isBlank()) {
            showError("Please select role.");
            return;
        }

        if ("SELLER".equalsIgnoreCase(role) && (storeName == null || storeName.isBlank())) {
            showError("Please enter store name for seller.");
            return;
        }

        if ("BIDDER".equalsIgnoreCase(role)) {
            storeName = null;
        }

        try {
            ClientSocket clientSocket = ClientSocket.getInstance();
            clientSocket.connect();

            RegisterRequest request = new RegisterRequest(
                    fullName,
                    username,
                    password,
                    confirmPassword,
                    role,
                    storeName
            );

            clientSocket.sendRequest(request);
            Object obj = clientSocket.receiveResponse();

            if (obj == null) {
                showError("No response from server.");
                return;
            }

            if (obj instanceof RegisterResponse response) {
                if (response.isSuccess()) {
                    Alert alert = new Alert(Alert.AlertType.INFORMATION);
                    alert.setTitle("Success");
                    alert.setHeaderText(null);
                    alert.setContentText(response.getMessage());
                    alert.showAndWait();

                    SceneUtil.changeScene(event, "/views/login_view.fxml", "Login");
                } else {
                    showError(response.getMessage());
                }
            } else {
                showError("Invalid response from server.");
            }

        } catch (Exception e) {
            e.printStackTrace();
            showError("Server connection error!");
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
