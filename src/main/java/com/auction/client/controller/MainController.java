package com.auction.client.controller;

import com.auction.server.dto.ItemDTO;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;

public class MainController {
    private static final String ROLE_SELLER = "SELLER";

    @FXML private Label welcomeLabel;
    @FXML private Label balanceLabel;
    @FXML private Button addBtn;
    @FXML private TableView<ItemDTO> productTable;
    @FXML private TableColumn<ItemDTO, String> colName;
    @FXML private TableColumn<ItemDTO, Double> colPrice;

}