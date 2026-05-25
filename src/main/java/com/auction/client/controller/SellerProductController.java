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
    @FXML private Label lblTimeHeader;
    @FXML private Button btnGoDetail;
    @FXML private Label lblStatusTag;

    private Timeline countdownTimeline;
    private volatile boolean watching = false;

    private Runnable onViewDetail;
    private String currentImagePath;

    public void setData(SellerHistoryItemDTO session) {
        lblProductName.setText(safe(session.getProductName()));
        lblSessionID.setText(safe(session.getSessionId()));
        lblProductType.setText(safe(session.getProductType()));
        lblStatusTag.setText(safe(session.getStatus()));

        int totalBids = session.getTotalBidsReceived();
        lblTotalBids.setText(String.format("%,d %s", totalBids, totalBids == 1 ? "bid" : "bids"));

        lblStartingPrice.setText(String.format("%.2f $", session.getStartingPrice()));
        lblCurrentPrice.setText(String.format("%.2f $", session.getCurrentPrice()));

        startCountdown(session.getStartTimeMillis(), session.getEndTimeMillis());
        updateStatusStyle(session.getStatus());
        currentImagePath = session.getImagePath();
        loadProductImage(currentImagePath);

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
        imgProduct.setImage(null);
        if (imagePath == null || imagePath.isBlank()) {
            return;
        }
        String requestedImagePath = imagePath;
        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                return ProductService.getInstance().getItemImage(requestedImagePath);
            }
        };

        task.setOnSucceeded(event -> {
            if (!requestedImagePath.equals(currentImagePath)) {
                return;
            }
            byte[] imageBytes = task.getValue();

            if (imageBytes == null || imageBytes.length == 0) {
                return;
            }

            imgProduct.setImage(
                    new Image(new ByteArrayInputStream(imageBytes), 150, 150, false, true)
            );
        });

//        task.setOnFailed(event -> {
//            Throwable ex = task.getException();
//            if (ex != null) {
//                ex.printStackTrace();
//            }
//        });

        task.setOnFailed(e -> {
            if (requestedImagePath.equals(currentImagePath)) {
                imgProduct.setImage(null);
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private void startCountdown(long startTimeMillis, long endTimeMillis) {
        if (countdownTimeline != null) {
            countdownTimeline.stop();
            countdownTimeline = null;
        }
        updateCountdownLabel(startTimeMillis, endTimeMillis);

        // Chạy ngầm cập nhật mỗi giây
        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateCountdownLabel(startTimeMillis, endTimeMillis);
        }));

        countdownTimeline.setCycleCount(Timeline.INDEFINITE);
        countdownTimeline.play();
    }

    private void updateCountdownLabel(long startTimeMillis, long endTimeMillis) {
        long now = System.currentTimeMillis();

        // Trường hợp phiên OPEN -> đếm ngược startTime
        if (startTimeMillis > 0 && startTimeMillis > now) {
            long remaining = startTimeMillis - now;
            long h = remaining / 3_600_000;
            long m = (remaining % 3_600_000) / 60_000;
            long s = (remaining % 60_000) / 1_000;
            lblTimeHeader.setText("Starts in: ");
            lblTimeRemaining.setText(String.format("%02d:%02d:%02d", h, m, s));
            return;
        }

        // Trường hợp phiên RUNNING → đếm ngược endTime
        long remaining = endTimeMillis - now;
        if (remaining > 0) {
            long h = remaining / 3_600_000;
            long m = (remaining % 3_600_000) / 60_000;
            long s = (remaining % 60_000) / 1_000;
            lblTimeHeader.setText("Time left: ");
            lblTimeRemaining.setText(String.format("%02d:%02d:%02d", h, m, s));
        } else {
            lblTimeHeader.setText("Status:");
            lblTimeRemaining.setText("EXPIRED");
            if ("OPEN".equalsIgnoreCase(lblStatusTag.getText()) || "RUNNING".equalsIgnoreCase(lblStatusTag.getText())) {
                lblStatusTag.setText("FINISHED");
                updateStatusStyle("FINISHED");
            }
            if (countdownTimeline != null) countdownTimeline.stop();
        }
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}
