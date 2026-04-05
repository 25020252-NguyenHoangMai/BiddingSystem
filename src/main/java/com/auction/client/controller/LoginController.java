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
        loginMessageLabel.setText("Invalid Login. Please try again.");
        loginMessageLabel.setVisible(true);
    }
}
