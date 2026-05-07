package com.auction.model;

public class Admin extends User {
    private static final long serialVersionUID = 1L;

    public Admin() {
        super();
    }

    public Admin(String id, String username, String password, String fullName) {
        super(id, username, password, fullName, "ADMIN");
    }
}