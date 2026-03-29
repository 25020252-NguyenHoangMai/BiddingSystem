package com.auction.server.model;

public class Seller extends User {
    private String storeName;
    public Seller() {
        super();
    }

    public Seller(String id, String username, String password, String fullName, String storeName) {
        super(id, username, password, fullName, "SELLER");
        this.storeName = storeName;
    }

    public String getStoreName() {
        return storeName;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}