package com.auction.model;

public abstract class Item extends Entity {
    private static final long serialVersionUID = 1L;
    private String name;
    private String description;
    private String itemType;
    private String sellerId;
    private double startingPrice;
    private String imagePath;


    public Item() { super(); }

    public Item(String id, String name, String imagePath, String sellerId){
        super(id);
        this.name = name;
        this.imagePath = imagePath;
        this.sellerId = sellerId;
    }

    public Item(String id, String name, String description, String imagePath, String itemType, String sellerId, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.imagePath = imagePath;
        this.itemType = itemType;
        this.sellerId = sellerId;
        this.startingPrice = startingPrice;
    }


    public abstract String getCategoryDetails();

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

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

    public String getImagePath() {
        return imagePath;
    }

    public void setImagePath(String imagePath) {
        this.imagePath = imagePath;
    }
}
