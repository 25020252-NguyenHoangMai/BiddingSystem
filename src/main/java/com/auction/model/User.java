package com.auction.model;

public abstract class User extends Entity {
    private String username;
    private String password;
    private String fullName;
    private String role;  //"BIDDER", "SELLER", "ADMIN"

    public User() {
        super();
    }

    public User(String id, String username, String password, String fullName, String role) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
        this.role = role;
    }
    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }
    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }
    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }
}

