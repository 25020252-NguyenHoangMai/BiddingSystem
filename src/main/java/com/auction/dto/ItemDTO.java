package com.auction.dto;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;

import java.io.Serializable;

public class ItemDTO implements Serializable {
    private static final long serialVersionUID = 1L;

    private String id;
    private String name;
    private String description;
    private String itemType;
    private String sellerId;
    private String sellerUsername;
    private double startingPrice;
    private double currentPrice;
    private String model;
    private String engineType;
    private int mileage;
    private String brand;
    private String artist;
    private long startTimeMillis;
    private long endTimeMillis;
    private int durationHours;
    private String sessionId;
    private String currentWinnerUsername;
    private String sessionStatus;
    private Double minimumNextBid;
    private String material;
    private String gemstone;
    private String weight;


    private transient BooleanProperty selected = new SimpleBooleanProperty(false);

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getItemType() {
        return itemType;
    }

    public void setItemType(String itemType) {
        this.itemType = itemType;
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

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getEngineType() {
        return engineType;
    }

    public void setEngineType(String engineType) {
        this.engineType = engineType;
    }

    public int getMileage() {
        return mileage;
    }

    public void setMileage(int mileage) {
        this.mileage = mileage;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }

    public double getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(double currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BooleanProperty selectedProperty() {
        // Nếu null (do vừa bay qua mạng về), thì khởi tạo mới
        if (selected == null) {
            selected = new SimpleBooleanProperty(false);
        }
        return selected;
    }

    public boolean isSelected() {return selected.get();}

    public void setSelected(boolean selected) { this.selected.set(selected);}

    public long getStartTimeMillis() {
        return startTimeMillis;
    }

    public void setStartTimeMillis(long startTimeMillis) {
        this.startTimeMillis = startTimeMillis;
    }

    public long getEndTimeMillis() { return endTimeMillis; }

    public void setEndTimeMillis(long endTimeMillis) { this.endTimeMillis = endTimeMillis; }

    public int getDurationHours() { return durationHours; }

    public void setDurationHours(int durationHours) { this.durationHours = durationHours; }

    public String getSessionId() { return sessionId; }

    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getCurrentWinnerUsername() { return currentWinnerUsername; }

    public void setCurrentWinnerUsername(String u) { this.currentWinnerUsername = u; }

    public String getSessionStatus() { return sessionStatus; }

    public void setSessionStatus(String s) { this.sessionStatus = s; }

    public String getMaterial() {
        return material;
    }

    public void setMaterial(String material) {
        this.material = material;
    }

    public String getGemstone() {
        return gemstone;
    }

    public void setGemstone(String gemstone) {
        this.gemstone = gemstone;
    }

    public String getWeight() {
        return weight;
    }

    public void setWeight(String weight) {
        this.weight = weight;
    }

    public String calculateTimeLeft() {
        long now = System.currentTimeMillis();

        if (startTimeMillis > 0 && now < startTimeMillis) {
            long diff = startTimeMillis - now;
            return "Starts in " + formatDuration(diff);
        }

        long diff = endTimeMillis - now;

        if (diff <= 0) return "Đã kết thúc";

        return formatDuration(diff);
    }

    private String formatDuration(long millis) {
        long seconds = (millis / 1000) % 60;
        long minutes = (millis / (1000 * 60)) % 60;
        long hours = millis / (1000 * 60 * 60);

        return String.format("%02d:%02d:%02d", hours, minutes, seconds);
    }

    public boolean hasStarted() {
        return startTimeMillis > 0 && System.currentTimeMillis() >= startTimeMillis;
    }

    public boolean hasEnded() {
        return endTimeMillis > 0 && System.currentTimeMillis() >= endTimeMillis;
    }

    public Double getMinimumNextBid() { return minimumNextBid;}

    public void setMinimumNextBid(Double minimumNextBid) { this.minimumNextBid = minimumNextBid;}



}
