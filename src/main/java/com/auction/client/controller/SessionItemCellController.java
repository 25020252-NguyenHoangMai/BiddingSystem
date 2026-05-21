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

    public void setData(SessionHistoryItemDTO session) {
        lblProductName.setText(safe(session.getProductName()));
        lblSessionID.setText(safe(session.getSessionId()));
        lblProductType.setText(safe(session.getProductType()));

        lblSellerInfo.setText(
                safe(session.getSellerUsername()) + " (" + safe(session.getSellerFullName()) + ")"
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
        } else if ("WON".equalsIgnoreCase(status)) {
            lblStatusTag.setStyle("-fx-background-color: #e8f5e9; -fx-text-fill: #2e7d32; -fx-background-radius: 20; -fx-padding: 4 12; -fx-font-weight: bold;");
        } else if ("RUNNING".equalsIgnoreCase(status)) {
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

    private String safe(String value) {
        return value == null ? "" : value;
    }
}