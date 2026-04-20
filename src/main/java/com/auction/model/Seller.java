package com.auction.model;

import java.io.Serial;
import java.io.Serializable;

public class Seller extends User implements Serializable {
    @Serial
    private static final long serialVersionUID = 1L;
    public Seller() {
        super();
    }

    public Seller(String id, String username, String password, String fullName) {
        super(id, username, password, fullName, "SELLER");
    }

}