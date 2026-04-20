package com.auction.model;

import java.io.Serializable;

public class Seller extends User implements Serializable {
    private static final long serialVersionUID = 1L;
    public Seller() {
        super();
    }

    public Seller(String id, String username, String password, String fullName, String storeName) {
        super(id, username, password, fullName, "SELLER");
    }

}