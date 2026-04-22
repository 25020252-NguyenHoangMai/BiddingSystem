package com.auction.client.controller;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextField;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.event.ActionEvent;
import javafx.scene.control.Label;

import com.auction.client.network.ClientSocket;
import com.auction.client.ClientSession;
import com.auction.request.LoginRequest;
import com.auction.response.LoginResponse;

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

        loginMessageLabel.setVisible(false);

        if (username == null || username.isBlank()) {
            loginMessageLabel.setText("Please enter username.");
            loginMessageLabel.setVisible(true);
            return;
        }

        if (password == null || password.isBlank()) {
            loginMessageLabel.setText("Please enter password.");
            loginMessageLabel.setVisible(true);
            return;
        }

        try {
            ClientSocket clientSocket = ClientSocket.getInstance();
            clientSocket.connect();

            LoginRequest request = new LoginRequest(username, password);
            clientSocket.sendRequest(request);

            Object obj = clientSocket.receiveResponse();

            if (obj == null) {
                loginMessageLabel.setText("Server không phản hồi");
                loginMessageLabel.setVisible(true);
                return;
            }

            if (obj instanceof LoginResponse response) {
                if (response.isSuccess()) {
                    System.out.println("Đăng nhập thành công!");

                    ClientSession.setCurrentUser(response.getUser());

                    com.auction.client.util.SceneUtil.changeScene(
                            event,
                            "/views/main.fxml",
                            "Auction Dashboard"
                    );
                } else {
                    loginMessageLabel.setText(response.getMessage());
                    loginMessageLabel.setVisible(true);
                }
            } else {
                loginMessageLabel.setText("Server phản hồi không hợp lệ");
                loginMessageLabel.setVisible(true);
            }

        } catch (Exception e) {
            e.printStackTrace();
            loginMessageLabel.setText("Lỗi kết nối Server!");
            loginMessageLabel.setVisible(true);
        }
    }

    @FXML
    private void handleRegisterAction(ActionEvent event) {
        com.auction.client.util.SceneUtil.changeScene(event, "/views/register_view.fxml", "Register");
    }
}
