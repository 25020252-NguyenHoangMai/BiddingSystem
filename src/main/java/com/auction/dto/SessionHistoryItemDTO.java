package com.auction.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class SessionHistoryItemDTO implements Serializable {
    private String sessionId;
    private String productName;
    private String productType;
    private String sellerId;
    private String sellerUsername;
    private double userLastBid;
    private double currentPrice;
    private LocalDateTime lastBidTime;
    private String status; // RUNNING, WON, CANCELED
    private String imagePath;

    public String getSessionId() {
        return sessionId;
    }

    public void setSessionId(String sessionId) {
        this.sessionId = sessionId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public String getSellerId() {
        return sellerId;
    }

    public void setSellerId(String sellerId) {
        this.sellerId = sellerId;
    }

    public String getSellerUsername() {
        return sellerUsername;
    }

    public void setSellerUsername(String sellerUsername) {
        this.sellerUsername = sellerUsername;
    }

    public double getUserLastBid() {
        return userLastBid;
    }

    public void setUserLastBid(double userLastBid) {
        this.userLastBid = userLastBid;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public LocalDateTime getLastBidTime() {
        return lastBidTime;
    }

    public void setLastBidTime(LocalDateTime lastBidTime) {
        this.lastBidTime = lastBidTime;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
