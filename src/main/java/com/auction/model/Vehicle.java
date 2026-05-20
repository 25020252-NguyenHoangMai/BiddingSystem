package com.auction.model;

public class Vehicle extends Item {

    private static final long serialVersionUID = 1L;

    private String brand;      // Hãng xe
    private String model;      // Dòng xe
    private String engineType; // Loại động cơ
    private int mileage;       // Số km đã đi

    public Vehicle() {
        super();
    }

    public Vehicle(String id, String name, String description, String imagePath, String sellerId, double startingPrice,
                   String brand, String model, String engineType, int mileage) {


        super(id, name, description, imagePath, "VEHICLE", sellerId, startingPrice);


        this.brand = brand;
        this.model = model;
        this.engineType = engineType;
        this.mileage = mileage;
    }

    @Override
    public String getCategoryDetails() {
        return "Phương tiện - Hãng: " + brand + ", Dòng xe: " + model + ", Động cơ: " + engineType + ", ODO: " + mileage + "km";
    }


    public String getBrand() { return brand; }
    public void setBrand(String brand) { this.brand = brand; }

    public String getModel() { return model; }
    public void setModel(String model) { this.model = model; }

    public String getEngineType() { return engineType; }
    public void setEngineType(String engineType) { this.engineType = engineType; }

    public int getMileage() { return mileage; }
    public void setMileage(int mileage) { this.mileage = mileage; }
}