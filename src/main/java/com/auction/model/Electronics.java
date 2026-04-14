package com.auction.model;

public class Electronics extends Item {
    private String brand;

    public Electronics(String id, String name, String description, double startingPrice,  String brand) {
        super(id, name, description, "ELECTRONNICS", startingPrice);
        this.brand = brand;
    }

    @Override
    public String getCategoryDetails() {
        return "Điện tử - Thương hiệu: " + brand;
    }

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}
