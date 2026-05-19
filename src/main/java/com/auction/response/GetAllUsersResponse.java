package com.auction.response;

import com.auction.dto.UserSessionDTO;

import java.util.List;

public class GetAllUsersResponse extends Response {
    private final List<UserSessionDTO> users;

    public GetAllUsersResponse(boolean success, String message, List<UserSessionDTO> users) {
        super(success, message);
        this.users = users;
    }

    public List<UserSessionDTO> getUsers() {
        return users;
    }
}
