package com.auction.client;

import com.auction.client.network.ClientSocket;
import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class MainApp extends Application {
    @Override
    public void start(Stage primaryStage) throws Exception {
        ClientSocket.getInstance().connect();
        Parent root = FXMLLoader.load(getClass().getResource("/views/login_view.fxml"));

        primaryStage.setTitle("UET Bidding System");
        primaryStage.setScene(new Scene(root, 520, 400));
        primaryStage.show();
    }

    public static void main(String[] args) {
        launch(args);
    }
}
