package com.auction.model;

import java.io.Serializable;

public class Seller extends User implements Serializable {
    private static final long serialVersionUID = 1L;
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