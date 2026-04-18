package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.Stage;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;

import java.io.File;
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
        File brandingFile = new File("src/main/resources/images/welcome.png");
        Image brandingImage = new Image(brandingFile.toURI().toString());
        brandingImageView.setImage(brandingImage);

        Platform.runLater(() -> enterUsernameField.requestFocus());

        loginMessageLabel.setVisible(false);
    }

    @FXML
    public void loginButtonOnAction(ActionEvent event) {
        String username = enterUsernameField.getText();
        String password = enterPasswordField.getText();

        if (username.isEmpty()) {
            loginMessageLabel.setText("Please enter username.");
            loginMessageLabel.setVisible(true);
            return;
        }

        try {
            // Gửi yêu cầu Login sang Server
            System.out.println("Đang gửi yêu cầu đăng nhập sang Server...");
            com.auction.client.network.ClientSocket clientSocket = new com.auction.client.network.ClientSocket();

            // Gửi thông tin đăng nhập sang Server
            String[] loginData = {"LOGIN", username, password};
            Object response = clientSocket.sendRequest(loginData);

            // Xử lý phản hồi từ Server
            if ("SUCCESS".equals(response)) {
                System.out.println("Đăng nhập thành công!");
                // Chuyển sang màn hình chính (Dashboard/Main)
                com.auction.client.util.SceneUtil.changeScene(event, "/views/main_view.fxml", "Auction Dashboard");
            } else {
                loginMessageLabel.setText("Invalid Login. Please try again.");
                loginMessageLabel.setVisible(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            loginMessageLabel.setText("Server connection error!");
            loginMessageLabel.setVisible(true);
        }
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        com.auction.client.util.SceneUtil.changeScene(event, "/views/register_view.fxml", "Register");
    }
}
