package com.auction.response;

import com.auction.dto.UserSessionDTO;

public class EnableSellerResponse extends Response {
    private final UserSessionDTO userSession;

    public EnableSellerResponse(boolean success, String message, UserSessionDTO userSession) {
        super(success, message);
        this.userSession = userSession;
    }

    public UserSessionDTO getUserSession() {
        return userSession;
    }
}
