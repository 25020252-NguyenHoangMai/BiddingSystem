package com.auction.client.controller;

import com.auction.client.service.ProductService;
import com.auction.dto.ItemDTO;
import javafx.animation.Animation;
import javafx.animation.KeyFrame;
import javafx.animation.Timeline;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.util.Duration;

import java.io.ByteArrayInputStream;

public class AuctionItemCellController {

    @FXML private Label lblProductName;
    @FXML private Label lblSessionID;
    @FXML private Label lblProductType;
    @FXML private Label lblSellerInfo;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeLeft;
    @FXML private Label lblStatusTag;
    @FXML private ImageView imgProduct;
    @FXML private Button btnViewDetail;

    private Timeline countdownTimeline;
    private Runnable onViewDetail;

    public void setData(ItemDTO item) {
        lblProductName.setText(safe(item.getName()));
        lblSessionID.setText(safe(item.getSessionId()));
        lblProductType.setText(safe(item.getItemType()));
        lblSellerInfo.setText(safe(item.getSellerUsername()));
        lblStartingPrice.setText(String.format("%.2f $", item.getStartingPrice()));
        double displayPrice =
                (item.getCurrentWinnerUsername() == null
                        || item.getCurrentWinnerUsername().isBlank())
                        ? item.getStartingPrice()
                        : item.getCurrentPrice();
        lblCurrentPrice.setText(String.format("%.2f $", displayPrice));
        lblStatusTag.setText(safe(item.getSessionStatus()));
        updateStatusStyle(item.getSessionStatus());
        startCountdown(item.getEndTimeMillis());
        loadProductImage(item.getImagePath());

        btnViewDetail.setOnAction(e -> {
            if (onViewDetail != null) onViewDetail.run();
        });
    }

    public void setOnViewDetail(Runnable onViewDetail) {
        this.onViewDetail = onViewDetail;
    }

    public void stopCountdown() {
        if (countdownTimeline != null) countdownTimeline.stop();
    }

    private void startCountdown(long endTimeMillis) {
        if (countdownTimeline != null) countdownTimeline.stop();

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = endTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                long h = remaining / 3_600_000;
                long m = (remaining % 3_600_000) / 60_000;
                long s = (remaining % 60_000) / 1_000;
                lblTimeLeft.setText(String.format("%02d:%02d:%02d", h, m, s));
            } else {
                lblTimeLeft.setText("EXPIRED");
                if ("OPEN".equalsIgnoreCase(lblStatusTag.getText()) || "RUNNING".equalsIgnoreCase(lblStatusTag.getText())) {
                    lblStatusTag.setText("FINISHED");
                    updateStatusStyle("FINISHED");
                }
                countdownTimeline.stop();
            }
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownLabel(long endTimeMillis) {
        long remaining = endTimeMillis - System.currentTimeMillis();
        if (remaining > 0) {
            long h = remaining / 3_600_000;
            long m = (remaining % 3_600_000) / 60_000;
            long s = (remaining % 60_000) / 1_000;
            lblTimeLeft.setText(String.format("%02d:%02d:%02d", h, m, s));
        } else {
            lblTimeLeft.setText("EXPIRED");
            if ("OPEN".equalsIgnoreCase(lblStatusTag.getText()) || "RUNNING".equalsIgnoreCase(lblStatusTag.getText())) {
                lblStatusTag.setText("FINISHED");
                updateStatusStyle("FINISHED");
            }
            if (countdownTimeline != null) countdownTimeline.stop();
        }
    }

    private void updateStatusStyle(String status) {
        if (status == null) return;
        switch (status.toUpperCase()) {
            case "RUNNING" -> lblStatusTag.setStyle(
                    "-fx-background-color: #e1f5fe; -fx-text-fill: #01579b; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
            case "OPEN" -> lblStatusTag.setStyle(
                    "-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
            case "FINISHED", "CANCELED" -> lblStatusTag.setStyle(
                    "-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
            default -> lblStatusTag.setStyle(
                    "-fx-background-color: #eeeeee; -fx-text-fill: #424242; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        }
    }

    private void loadProductImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) return;

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return ProductService.getInstance().getItemImage(imagePath);
            }
        };

        task.setOnSucceeded(e -> {
            byte[] bytes = task.getValue();
            if (bytes != null && bytes.length > 0) {
                imgProduct.setImage(new Image(new ByteArrayInputStream(bytes), 150, 150, false, true));
            }
        });

        Thread t = new Thread(task);
        t.setDaemon(true);
        t.start();
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
