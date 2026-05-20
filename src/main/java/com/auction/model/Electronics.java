package com.auction.model;

public class Electronics extends Item {

    private static final long serialVersionUID = 1L;

    private String brand;

    public Electronics(){}

    public Electronics(String id, String name, String description, String imagePath, String sellerID, double startingPrice,  String brand) {
        super(id, name, description, imagePath, "ELECTRONICS", sellerID, startingPrice);
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
