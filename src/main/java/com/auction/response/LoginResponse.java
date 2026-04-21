package com.auction.response;

import com.auction.dto.UserSessionDTO;

public class LoginResponse extends Response {
    private UserSessionDTO user;

    public LoginResponse(boolean success, String message, UserSessionDTO user) {
        super(success, message);
        this.user = user;
    }

    public UserSessionDTO getUser() {
        return user;
    }
}