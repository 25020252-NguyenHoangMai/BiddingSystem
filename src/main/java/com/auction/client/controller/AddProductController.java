package com.auction.client.controller;

import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;

public class AddProductController {
    @FXML private TextField txtName;
    @FXML private TextArea txtDescription;
    @FXML private TextField txtPrice;
    @FXML private ComboBox<String> cbCategory;
    @FXML private VBox dynamicFields;
    @FXML private Label errorLabel;
    @FXML private Button btnSubmit;
}
