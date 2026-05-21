package com.auction.client.controller;

import com.auction.client.service.ProductService;
import com.auction.dto.SellerHistoryItemDTO;
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

public class SellerProductController {
    @FXML private ImageView imgProduct;
    @FXML private Label lblProductName;
    @FXML private Label lblSessionID;
    @FXML private Label lblProductType;
    @FXML private Label lblTotalBids;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeRemaining;
    @FXML private Button btnGoDetail;
    @FXML private Label lblStatusTag;

    private Timeline countdownTimeline;
    private volatile boolean watching = false;

    private Runnable onViewDetail;

    public void setData(SellerHistoryItemDTO session) {
        lblProductName.setText(safe(session.getProductName()));
        lblSessionID.setText(safe(session.getSessionId()));
        lblProductType.setText(safe(session.getProductType()));
        lblStatusTag.setText(safe(session.getStatus()));

        int totalBids = session.getTotalBidsReceived();
        lblTotalBids.setText(String.format("%,d %s", totalBids, totalBids == 1 ? "bid" : "bids"));

        lblStartingPrice.setText(String.format("%.2f $", session.getStartingPrice()));
        lblCurrentPrice.setText(String.format("%.2f $", session.getCurrentPrice()));

        startCountdown(session.getEndTimeMillis());
        updateStatusStyle(session.getStatus());
        loadProductImage(session.getImagePath());

        btnGoDetail.setOnAction(event -> {
            if (onViewDetail != null) {
                onViewDetail.run();
            }
        });
    }

    public void setOnViewDetail(Runnable onViewDetail) {
        this.onViewDetail = onViewDetail;
    }

    private void updateStatusStyle(String status) {
        if ("DELETED".equalsIgnoreCase(status)) {
            lblStatusTag.setStyle("-fx-background-color: #ffebee; -fx-text-fill: #c62828; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        } else if ("FINISHED".equalsIgnoreCase(status)) {
            lblStatusTag.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        } else if ("RUNNING".equalsIgnoreCase(status) || "OPEN".equalsIgnoreCase(status)) {
            lblStatusTag.setStyle("-fx-background-color: #e1f5fe; -fx-text-fill: #01579b; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        } else {
            lblStatusTag.setStyle("-fx-background-color: #eeeeee; -fx-text-fill: #424242; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        }
    }

    private void loadProductImage(String imagePath) {
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return ProductService.getInstance().getItemImage(imagePath);
            }
        };

        task.setOnSucceeded(event -> {
            byte[] imageBytes = task.getValue();

            if (imageBytes == null || imageBytes.length == 0) {
                return;
            }

            imgProduct.setImage(
                    new Image(new ByteArrayInputStream(imageBytes), 150, 150, false, true)
            );
        });

        task.setOnFailed(event -> {
            Throwable ex = task.getException();
            if (ex != null) {
                ex.printStackTrace();
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void startCountdown(long endTimeMillis) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            long remaining = endTimeMillis - System.currentTimeMillis();
            if (remaining > 0) {
                updateCountdownLabel(remaining);
            } else {
                handleAuctionExpired();
            }
        }));
        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownLabel(long remainingMillis) {
        long hours = remainingMillis / 3_600_000;
        long minutes = (remainingMillis % 3_600_000) / 60_000;
        long seconds = (remainingMillis % 60_000) / 1_000;

        lblTimeRemaining.setText(String.format("%02d:%02d:%02d", hours, minutes, seconds));
    }

    private void handleAuctionExpired() {
        lblTimeRemaining.setVisible(false); // Ẩn hiển thị
        lblTimeRemaining.setManaged(false);

        watching = false;

        if (countdownTimeline != null) {
            countdownTimeline.stop();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
