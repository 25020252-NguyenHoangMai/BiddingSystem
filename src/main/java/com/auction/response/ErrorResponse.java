package com.auction.response;

public class ErrorResponse extends Response {
    public ErrorResponse(String message) {
        super(false, message);
    }
}