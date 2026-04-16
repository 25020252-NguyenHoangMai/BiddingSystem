package com.auction.client.controller;

import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.Label;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import com.auction.client.util.SceneUtil;

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
        roleChoiceBox.getItems().addAll("Bidder", "Seller");

        roleChoiceBox.setValue("Bidder");

        storeNameLabel.setVisible(false);
        storeNameField.setVisible(false);
        storeNameLabel.setManaged(false);
        storeNameField.setManaged(false);

        roleChoiceBox.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            boolean isSeller = "Seller".equals(newVal);

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
        String username = UsernameTextField.getText();
        String password = setPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String fullName = fullNameTextField.getText();
        String selectedRole = roleChoiceBox.getValue();
        String storeName = storeNameField.getText();

        if (!password.equals(confirmPassword)) {
            System.out.println("Lỗi: Mật khẩu xác nhận không khớp");
            return;
        }

        if ("Bidder".equals(selectedRole)) {
            storeName = null;
        } else {
            if (storeName == null || storeName.trim().isEmpty()) {
                System.out.println("Lỗi: Seller phải có tên cửa hàng");
                return;
            }
        }

        System.out.println("User: " + username + " | Role: " + selectedRole);

        User newUser;
        if ("Bidder".equals(selectedRole)) {
            newUser = new Bidder();
        } else {
            newUser = new Seller();
            ((Seller) newUser).setStoreName(storeNameField.getText());
        }

        newUser.setFullName(fullNameTextField.getText());
        newUser.setFullName(fullNameTextField.getText());
        newUser.setUsername(UsernameTextField.getText());
        newUser.setPassword(setPasswordField.getText());
        newUser.setRole(roleChoiceBox.getValue());

        SceneUtil.changeScene(event, "/views/login_view.fxml", "Login");
    }
}
