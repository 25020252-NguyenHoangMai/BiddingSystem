package com.auction.response;

import com.auction.dto.UserSessionDTO;

public class GetCurrentUserResponse extends Response {
    private final UserSessionDTO userSession;

    public GetCurrentUserResponse(boolean success, String message, UserSessionDTO userSession) {
        super(success, message);
        this.userSession = userSession;
    }

    public UserSessionDTO getUserSession() {
        return userSession;
    }
}
