package com.auction.model;

public abstract class Item extends Entity {
    private String name;
    private String description;
    private String itemType;
    private String sellerID;
    private double startingPrice;

    public Item() { super(); }

    public Item(String id, String name, String description, String itemType, String sellerID, double startingPrice) {
        super(id);
        this.name = name;
        this.description = description;
        this.itemType = itemType;
        this.sellerID = sellerID;
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

    public String getSellerID() {
        return sellerID;
    }

    public void setSellerID(String sellerID) {
        this.sellerID = sellerID;
    }

    public double getStartingPrice() {
        return startingPrice;
    }

    public void setStartingPrice(double startingPrice) {
        this.startingPrice = startingPrice;
    }

}
