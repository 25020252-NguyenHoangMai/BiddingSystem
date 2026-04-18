package com.auction.response;

import com.auction.model.User;

public class LoginResponse extends Response {
    private User user;

    public LoginResponse(boolean success, String message, User user) {
        super(success, message);
        this.user = user;
    }

    public User getUser() {
        return user;
    }
}