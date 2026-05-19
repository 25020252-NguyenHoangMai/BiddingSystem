package com.auction.request;

public class EditProfileRequest extends Request {
    private String fullName;
    private String username;
    private String password;
    private String avatarUrl;

    public EditProfileRequest(String fullName, String username, String password, String avatarUrl) {
        this.fullName = fullName;
        this.username = username;
        this.password = password;
        this.avatarUrl = avatarUrl;
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

    public String getAvatarUrl() {
        return avatarUrl;
    }
}
