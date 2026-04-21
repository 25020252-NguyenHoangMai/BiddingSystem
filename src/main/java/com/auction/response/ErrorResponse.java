package com.auction.response;

import com.auction.response.Response;

public class ErrorResponse extends Response {
    public ErrorResponse(String message) {
        super(false, message);
    }
}