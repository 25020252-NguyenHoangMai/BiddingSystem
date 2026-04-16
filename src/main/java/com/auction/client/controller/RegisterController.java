package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.ChoiceBox;
import javafx.scene.control.PasswordField;
import javafx.scene.control.TextField;
import javafx.event.ActionEvent;
import com.auction.client.util.SceneUtil;

public class RegisterController {

    @FXML private TextField UsernameTextField;
    @FXML private PasswordField setPasswordField;
    @FXML private PasswordField confirmPasswordField;

    @FXML private ChoiceBox<String> roleChoiceBox;

    @FXML
    public void initialize() {
        roleChoiceBox.getItems().addAll("Bidder", "Seller");

        roleChoiceBox.setValue("Bidder");
    }

    @FXML
    private void handleRegister(ActionEvent event) {
        String username = UsernameTextField.getText();
        String password = setPasswordField.getText();

        String selectedRole = roleChoiceBox.getValue();

        System.out.println("User: " + username + " | Role: " + selectedRole);

        // Thực hiện logic gửi dữ liệu lên Server ở đây...

        SceneUtil.changeScene(event, "/views/login_view.fxml", "Login");
    }
}
