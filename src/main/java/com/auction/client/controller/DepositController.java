package com.auction.client.controller;

import com.auction.client.ClientSession;
import com.auction.client.service.UserClientService;
import com.auction.dto.UserSessionDTO;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

public class DepositController {

    @FXML private Label currentBalanceLabel;
    @FXML private TextField amountField;
    @FXML private Label errorLabel;
    @FXML private Button confirmBtn;
    @FXML private Button cancelBtn;

    private final UserClientService userClientService = UserClientService.getInstance();

    @FXML
    public void initialize() {
        UserSessionDTO user = ClientSession.getCurrentUser();
        if (user != null) {
            currentBalanceLabel.setText(String.format("%.2f $", user.getBalance()));
        }

        // Chỉ cho phép nhập số và dấu chấm
        amountField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (!newVal.matches("\\d*\\.?\\d*")) {
                amountField.setText(oldVal);
            }
            errorLabel.setText("");
        });
    }

    @FXML
    private void handleQuickAmount(javafx.event.ActionEvent event) {
        Button btn = (Button) event.getSource();
        String userData = (String) btn.getUserData();
        try {
            double quickAmount = Double.parseDouble(userData);
            double current = 0;
            try {
                String text = amountField.getText().trim();
                if (!text.isEmpty()) current = Double.parseDouble(text);
            } catch (NumberFormatException ignored) {}
            amountField.setText(String.valueOf((int)(current + quickAmount)));
            errorLabel.setText("");
        } catch (NumberFormatException ignored) {}
    }

    @FXML
    private void handleConfirm() {
        String amountText = amountField.getText().trim();

        if (amountText.isEmpty()) {
            errorLabel.setText("Please enter the amount you deposit.");
            return;
        }

        double amount;
        try {
            amount = Double.parseDouble(amountText);
        } catch (NumberFormatException e) {
            errorLabel.setText("The amount is invalid.");
            return;
        }

        if (amount <= 0) {
            errorLabel.setText("The amount must be greater than 0.");
            return;
        }

        UserSessionDTO user = ClientSession.getCurrentUser();
        if (user == null) {
            errorLabel.setText("Login session has expired.");
            return;
        }

        // Disable UI trong khi xử lý
        confirmBtn.setDisable(true);
        cancelBtn.setDisable(true);
        confirmBtn.setText("Processing...");

        final double finalAmount = amount;

        Task<UserSessionDTO> task = new Task<>() {
            @Override
            protected UserSessionDTO call() throws Exception {
                return userClientService.deposit(user.getId(), finalAmount);
            }
        };

        task.setOnSucceeded(e -> {
            UserSessionDTO updatedUser = task.getValue();
            // Cập nhật ClientSession với thông tin mới
            ClientSession.setCurrentUser(updatedUser);

            Platform.runLater(() -> {
                showSuccess(String.format("Deposit successful!\nNew balance: %.2f $", updatedUser.getBalance()));
                closeWindow();
            });
        });

        task.setOnFailed(e -> {
            Platform.runLater(() -> {
                confirmBtn.setDisable(false);
                cancelBtn.setDisable(false);
                confirmBtn.setText("Confirm");
                String msg = task.getException() != null ? task.getException().getMessage() : "Unknown Error";
                errorLabel.setText(msg);
            });
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    @FXML
    private void handleCancel() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) amountField.getScene().getWindow();
        stage.close();
    }

    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Deposit successful");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
