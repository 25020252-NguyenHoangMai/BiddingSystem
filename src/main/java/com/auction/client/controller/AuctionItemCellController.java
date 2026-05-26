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
import com.auction.client.util.ImageCache;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import java.io.ByteArrayInputStream;

public class AuctionItemCellController {

    @FXML private Label lblProductName;
    @FXML private Label lblSessionID;
    @FXML private Label lblProductType;
    @FXML private Label lblSellerInfo;
    @FXML private Label lblStartingPrice;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblTimeHeader;
    @FXML private Label lblTimeLeft;
    @FXML private Label lblStatusTag;
    @FXML private ImageView imgProduct;
    @FXML private Button btnViewDetail;

    private Timeline countdownTimeline;
    private Runnable onViewDetail;
    private String currentImagePath;
    private static final ExecutorService IMAGE_EXECUTOR = Executors.newFixedThreadPool(3);

    public void setData(ItemDTO item) {
        lblProductName.setText(safe(item.getName()));
        lblSessionID.setText(safe(item.getSessionId()));
        lblProductType.setText(safe(item.getItemType()));
        lblSellerInfo.setText(
                item.getSellerUsername() != null
                        ? item.getSellerUsername()
                        : "Unknown");
        lblStartingPrice.setText(String.format("%.2f $", item.getStartingPrice()));
        double displayPrice =
                (item.getCurrentWinnerUsername() == null
                        || item.getCurrentWinnerUsername().isBlank())
                        ? item.getStartingPrice()
                        : item.getCurrentPrice();
        lblCurrentPrice.setText(String.format("%.2f $", displayPrice));
        lblStatusTag.setText(safe(item.getSessionStatus()));
        updateStatusStyle(item.getSessionStatus());
        startCountdown(item.getStartTimeMillis(), item.getEndTimeMillis());
        currentImagePath = item.getImagePath();
        loadProductImage(currentImagePath);

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

    private void startCountdown(long startTimeMillis, long endTimeMillis) {
        if (countdownTimeline != null) countdownTimeline.stop();

        // Hiển thị ngay lập tức, không chờ 1 giây
        updateCountdownLabel(startTimeMillis, endTimeMillis);

        countdownTimeline = new Timeline(new KeyFrame(Duration.seconds(1), e -> {
            updateCountdownLabel(startTimeMillis, endTimeMillis);
        }));
        countdownTimeline.setCycleCount(Animation.INDEFINITE);
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
            lblTimeLeft.setText(String.format("%02d:%02d:%02d", h, m, s));
            return;
        }

        // Trường hợp phiên RUNNING → đếm ngược endTime
        long remaining = endTimeMillis - now;
        if (remaining > 0) {
            long h = remaining / 3_600_000;
            long m = (remaining % 3_600_000) / 60_000;
            long s = (remaining % 60_000) / 1_000;
            lblTimeHeader.setText("Time left: ");
            lblTimeLeft.setText(String.format("%02d:%02d:%02d", h, m, s));
        } else {
            lblTimeHeader.setText("Status:");
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

    // THAY THẾ method loadProductImage cũ bằng:
    private void loadProductImage(String imagePath) {
        imgProduct.setImage(null);

        if (imagePath == null || imagePath.isBlank()) return;

        if (ImageCache.contains(imagePath)) {
            byte[] cached = ImageCache.get(imagePath);
            if (cached != null && cached.length > 0) {
                imgProduct.setImage(new Image(new ByteArrayInputStream(cached), 150, 150, false, true));
                return;
            }
        }

        final String requestedImagePath = imagePath;

        Task<byte[]> task = new Task<>() {
            @Override
            protected byte[] call() throws Exception {
                // Kiểm tra lại cache trong background (tránh race condition)
                byte[] cached = ImageCache.get(requestedImagePath);
                if (cached != null) return cached;

                byte[] bytes = ProductService.getInstance().getItemImage(requestedImagePath);
                if (bytes != null && bytes.length > 0) {
                    ImageCache.put(requestedImagePath, bytes);
                }
                return bytes;
            }
        };

        task.setOnSucceeded(e -> {
            if (!requestedImagePath.equals(currentImagePath)) return; // cell đã bị recycle
            byte[] bytes = task.getValue();
            if (bytes != null && bytes.length > 0) {
                imgProduct.setImage(new Image(new ByteArrayInputStream(bytes), 150, 150, false, true));
            }
        });

        task.setOnFailed(e -> {
            if (requestedImagePath.equals(currentImagePath)) {
                imgProduct.setImage(null);
            }
        });

        // Dùng pool cố định thay vì tạo thread mới mỗi lần
        IMAGE_EXECUTOR.submit(task);
    }

    private String safe(String v) {
        return v == null ? "" : v;
    }
}
