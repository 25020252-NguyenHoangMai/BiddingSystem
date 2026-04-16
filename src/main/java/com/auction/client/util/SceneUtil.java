package com.auction.client.util;

import javafx.event.ActionEvent;
import javafx.fxml.FXMLLoader;
import javafx.scene.Node;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import java.io.IOException;

public class SceneUtil {
    public static void changeScene(ActionEvent event, String fxmlPath, String title) {
        try {
            Parent root = FXMLLoader.load(SceneUtil.class.getResource(fxmlPath));

            Stage stage = (Stage) ((Node) event.getSource()).getScene().getWindow();

            stage.setTitle(title);
            stage.setScene(new Scene(root));
            stage.show();
        } catch (IOException e) {
            System.err.println("Error: cannot find file FXML at path: " + fxmlPath);
            e.printStackTrace();
        }
    }
}
