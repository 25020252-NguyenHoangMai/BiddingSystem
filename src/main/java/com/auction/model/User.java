package com.auction.model;

public abstract class User extends Entity{
    private static final long serialVersionUID = 1L;
    private String username;
    private String password;
    private String fullName;

    public User() {
        super();
    }

    public User(String id, String username, String password, String fullName) {
        super(id);
        this.username = username;
        this.password = password;
        this.fullName = fullName;
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

}

