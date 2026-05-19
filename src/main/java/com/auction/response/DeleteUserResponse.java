package com.auction.response;

import com.auction.dto.UserSessionDTO;

public class DeleteUserResponse extends Response {
    public DeleteUserResponse(boolean success, String message) {
        super(success, message);
    }
}
