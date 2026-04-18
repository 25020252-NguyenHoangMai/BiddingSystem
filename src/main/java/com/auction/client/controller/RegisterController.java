package com.auction.client.controller;

import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dao.UserDAO;
import javafx.fxml.FXML;
import javafx.scene.control.*;
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
        String username = UsernameTextField.getText();
        String password = setPasswordField.getText();
        String confirmPassword = confirmPasswordField.getText();
        String fullName = fullNameTextField.getText();
        String selectedRole = roleChoiceBox.getValue();
        String storeName = storeNameField.getText();

        if (username.isEmpty() || password.isEmpty() || !password.equals(confirmPassword)) {
            System.out.println("Vui lòng kiểm tra lại thông tin hoặc mật khẩu không khớp");
            return;
        }

        if ("BIDDER".equals(selectedRole)) {
            storeName = null;
        } else {
            if (storeName == null || storeName.trim().isEmpty()) {
                System.out.println("Lỗi: Seller phải có tên cửa hàng");
                return;
            }
        }

        System.out.println("User: " + username + " | Role: " + selectedRole);

        User newUser;
        if ("BIDDER".equals(selectedRole)) {
            newUser = new Bidder();
            ((Bidder) newUser).setBalance(0.0);
        } else {
            newUser = new Seller();
            ((Seller) newUser).setStoreName(storeName);
        }

        newUser.setFullName(fullName);
        newUser.setUsername(username);
        newUser.setPassword(password);
        newUser.setRole(selectedRole);

        try {
            UserDAO userDAO = new UserDAO();
            userDAO.register(newUser);

            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Success");
            alert.setHeaderText(null);
            alert.setContentText("Đăng ký tài khoản thành công! Bạn có thể đăng nhập ngay bây giờ.");
            alert.showAndWait(); // Dừng lại đợi người dùng bấm OK mới chạy tiếp

            // Chuyển về màn hình Login
            SceneUtil.changeScene(event, "/views/login_view.fxml", "Login");

        } catch (Exception e) {
            System.err.println("Lỗi đăng ký: " + e.getMessage());
        }
    }
}
