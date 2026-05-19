package com.auction.response;

import com.auction.dto.UserSessionDTO;

public class EditProfileResponse extends Response {
    private final UserSessionDTO userSessionDTO;

    public EditProfileResponse(boolean success, String message, UserSessionDTO userSessionDTO) {
        super(success, message);
        this.userSessionDTO = userSessionDTO;
    }

    public UserSessionDTO userSessionDTO() {
        return userSessionDTO;
    }
}