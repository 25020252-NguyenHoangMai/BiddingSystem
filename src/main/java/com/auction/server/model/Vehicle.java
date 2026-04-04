package com.auction.server.model;
import java.time.LocalDateTime;

public class Vehicle extends Item {
    private String model;      // Dòng xe (ví dụ: Civic, Vios)
    private String engineType; // Loại động cơ (Xăng, Điện)
    private int mileage;       // Số km đã đi

    public Vehicle() {
        super();
    }

    public Vehicle(String id, String name, String description, double startingPrice, String model, String engineType, int mileage) {
        super(id, name, description, startingPrice);
        this.model = model;
        this.engineType = engineType;
        this.mileage = mileage;
    }

    @Override
    public String getCategoryDetails() {
        return "Phương tiện - Dòng xe: " + model + ", Động cơ: " + engineType + ", ODO: " + mileage + "km";
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
}
