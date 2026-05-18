package com.auction.model;

public class Jewelry extends Item {

    private static final long serialVersionUID = 1L;

    private String material;
    private String gemstone;
    private String weight;
    private String brand;

    public Jewelry() {
    }

    public Jewelry(String id, String name, String description, String sellerId, double startingPrice,  String material, String gemstone, String weight, String brand){
        super(id, name, description, "JEWELRY", sellerId, startingPrice);
        this.material = material;
        this.gemstone = gemstone;
        this.weight = weight;
        this.brand = brand;
    }

    public String getCategoryDetails() {

        StringBuilder details = new StringBuilder();

        if (brand != null && !brand.isBlank()) {
            details.append("Thương hiệu: ").append(brand).append(" | ");
        }
        if (material != null && !material.isBlank()) {
            details.append("Chất liệu: ").append(material).append(" | ");
        }
        if (gemstone != null && !gemstone.isBlank()) {
            details.append("Đá quý: ").append(gemstone).append(" | ");
        }
        if (weight != null && !weight.isBlank()) {
            details.append("Trọng lượng: ").append(weight);
        }

        String result = details.toString().trim();
        if (result.endsWith("|")) {
            result = result.substring(0, result.length() - 1).trim();
        }


        return result.isEmpty() ? "Chưa cập nhật chi tiết trang sức" : result;
    }

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

    public String getBrand() {
        return brand;
    }

    public void setBrand(String brand) {
        this.brand = brand;
    }
}