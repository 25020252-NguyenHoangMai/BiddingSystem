package com.auction.client.controller;

import com.auction.client.service.ProductService;
import com.auction.dto.SessionHistoryItemDTO;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;

import java.io.ByteArrayInputStream;
import java.time.format.DateTimeFormatter;

public class SessionItemCellController {
    private static final DateTimeFormatter DATE_TIME_FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    @FXML private Label lblProductName;
    @FXML private Label lblSessionID;
    @FXML private Label lblProductType;
    @FXML private Label lblSellerInfo;
    @FXML private Label lblUserLastBid;
    @FXML private Label lblCurrentPrice;
    @FXML private Label lblLastBidTime;
    @FXML private Label lblStatusTag;
    @FXML private ImageView imgProduct;
    @FXML private Button btnGoDetail;

    private Runnable onViewDetail;
    private String currentImagePath;

    public void setData(SessionHistoryItemDTO session) {
        lblProductName.setText(safe(session.getProductName()));
        lblSessionID.setText(safe(session.getSessionId()));
        lblProductType.setText(safe(session.getProductType()));

        lblSellerInfo.setText(
                safe(session.getSellerUsername())
        );

        lblUserLastBid.setText(String.format("%.2f $", session.getUserLastBid()));
        lblCurrentPrice.setText(String.format("%.2f $", session.getCurrentPrice()));

        lblLastBidTime.setText(
                session.getLastBidTime() != null
                        ? session.getLastBidTime().format(DATE_TIME_FORMATTER)
                        : "N/A"
        );

        lblStatusTag.setText(safe(session.getStatus()));
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
        } else if ("WON".equalsIgnoreCase(status)) {
            lblStatusTag.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        } else if ("RUNNING".equalsIgnoreCase(status)) {
            lblStatusTag.setStyle("-fx-background-color: #e1f5fe; -fx-text-fill: #01579b; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        } else {
            lblStatusTag.setStyle("-fx-background-color: #eeeeee; -fx-text-fill: #424242; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        }
    }

    // Cập nhật realtime các thay đổi
    public void updateRealtime(SessionHistoryItemDTO session) {
        // Seller username (đổi tên)
        lblSellerInfo.setText(
                session.getSellerUsername() != null
                        ? session.getSellerUsername()
                        : "Unknown");

        // Your latest bid
        lblUserLastBid.setText(String.format("%.2f $", session.getUserLastBid()));

        // Current price
        lblCurrentPrice.setText(String.format("%.2f $", session.getCurrentPrice()));

        // Last bid time
        lblLastBidTime.setText(
                session.getLastBidTime() != null
                        ? session.getLastBidTime().format(DATE_TIME_FORMATTER)
                        : "N/A"
        );

        // Status
        lblStatusTag.setText(safe(session.getStatus()));
        updateStatusStyle(session.getStatus());

        // Product image — chỉ reload nếu path thay đổi
        String newImagePath = session.getImagePath();
        if (!java.util.Objects.equals(newImagePath, currentImagePath)) {
            currentImagePath = newImagePath;
            loadProductImage(currentImagePath);
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

        task.setOnFailed(e -> {
            if (requestedImagePath.equals(currentImagePath)) {
                imgProduct.setImage(null);
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}