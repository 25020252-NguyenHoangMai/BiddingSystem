package com.auction.request;

public class RegisterRequest extends Request {
    private final String fullName;
    private final String username;
    private final String password;
    private final String confirmPassword;
    private final String role;
    private final String storeName;

    public RegisterRequest(String fullName, String username, String password, String confirmPassword, String role, String storeName) {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.confirmPassword = confirmPassword;
        this.role = role;
        this.storeName = storeName;
    }

    public String getFullName() {
        return fullName;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getConfirmPassword() {
        return confirmPassword;
    }

    public String getRole() {
        return role;
    }

    public String getStoreName() {
        return storeName;
    }
}
