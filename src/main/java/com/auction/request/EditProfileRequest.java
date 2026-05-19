package com.auction.request;

public class EditProfileRequest extends Request {
    private String userId;
    private String fullName;
    private String username;
    private String password;

    public EditProfileRequest(String userId, String fullName, String username, String password) {
        this.userId = userId;
        this.fullName = fullName;
        this.username = username;
        this.password = password;
    }

    public String getUserId() {
        return userId;
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
}
