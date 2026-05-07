package com.auction.client.controller;

import com.auction.dto.ItemDTO;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.text.Text;
import javafx.stage.Stage;
import javafx.util.Duration;

import java.text.NumberFormat;
import java.util.Locale;

public class AuctionDetailController {

    @FXML private Label lblCategory, lblName, lblTimer, lblCurrentBid, lblLeadingUser,
            lblMinBidHint, lblBalance, lblSeller, lblStartingPrice, lblTotalBids;
    @FXML private Text txtDescription;
    @FXML private GridPane gridSpecs;
    @FXML private TextField txtBidAmount;
    @FXML private ListView<String> lvBidHistory;
    @FXML private Button btnBack, btnPlaceBid;

    private ItemDTO currentItem;
    private int timeLeft = 600; // Mặc định 10 phút đếm ngược
    private final NumberFormat fmt = NumberFormat.getCurrencyInstance(Locale.US);

    @FXML
    public void initialize() {
        // Mặc định số dư là 0 như yêu cầu
        lblBalance.setText("0.00 $");
        startTimer();

        // Giả lập danh sách lịch sử trống
        lvBidHistory.setPlaceholder(new Label("No history yet. Be the first to bid!"));
    }

    /**
     * Nhận dữ liệu từ màn hình Add Product gửi sang
     */
    public void setItemData(ItemDTO item) {
        if (item == null) return;
        this.currentItem = item;

        // 1. Thông tin cơ bản
        lblCategory.setText(item.getItemType());
        lblName.setText(item.getName());
        txtDescription.setText(item.getDescription());
        lblSeller.setText("User ID: #" + item.getSellerId());
        lblStartingPrice.setText(fmt.format(item.getStartingPrice()));
        lblCurrentBid.setText(fmt.format(item.getStartingPrice()));
        lblMinBidHint.setText("Min bid step: $10.00");

        // 2. Hiển thị đặc điểm riêng biệt dựa trên ItemType
        setupDynamicSpecs(item);
    }

    private void setupDynamicSpecs(ItemDTO item) {
        gridSpecs.getChildren().clear();
        int row = 0;

        String type = (item.getItemType() != null) ? item.getItemType().toUpperCase() : "";

        switch (type) {
            case "VEHICLE":
                addSpecRow("Model:", item.getModel(), row++);
                addSpecRow("Engine:", item.getEngineType(), row++);
                addSpecRow("Mileage:", item.getMileage() + " km", row++);
                break;

            case "ELECTRONICS":
                addSpecRow("Brand:", item.getBrand(), row++);
                break;

            case "ART":
                addSpecRow("Artist:", item.getArtist(), row++);
                break;
        }
    }

    private void addSpecRow(String label, String value, int row) {
        Label lbl = new Label(label);
        lbl.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");

        Label val = new Label(value != null ? value : "N/A");
        val.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");

        gridSpecs.add(lbl, 0, row);
        gridSpecs.add(val, 1, row);
    }

    private void startTimer() {
        Timeline timeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            if (timeLeft > 0) {
                timeLeft--;
                int mins = timeLeft / 60;
                int secs = timeLeft % 60;
                lblTimer.setText(String.format("%02d:%02d", mins, secs));
            } else {
                lblTimer.setText("EXPIRED");
                lblTimer.setStyle("-fx-text-fill: red; -fx-background-color: #ffebee;");
                btnPlaceBid.setDisable(true);
            }
        }));
        timeline.setCycleCount(Timeline.INDEFINITE);
        timeline.play();
    }

    @FXML
    private void handlePlaceBid(ActionEvent event) {
        // Logic đặt giá của bạn ở đây
        String amount = txtBidAmount.getText();
        if(!amount.isEmpty()) {
            System.out.println("Bid placed: " + amount);
        }
    }

    @FXML
    private void handleBack(ActionEvent event) {
        ((Stage) btnBack.getScene().getWindow()).close();
    }
}