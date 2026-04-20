package com.auction.request;

public class RegisterRequest extends Request {
    private String fullName;
    private String username;
    private String password;
    private String confirmPassword;
    private String role;
    private String storeName;

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

    public void setFullName (String newFullname) {
        this.fullName = newFullname;
    }

    public void setUsername(String newUsername) {
        this.username = newUsername;
    }

    public void setPassword(String newPassword) {
        this.password = newPassword;
    }

    public void setConfirmPassword(String newConfirmPassword) {
        this.confirmPassword = newConfirmPassword;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setStoreName(String storeName) {
        this.storeName = storeName;
    }
}
