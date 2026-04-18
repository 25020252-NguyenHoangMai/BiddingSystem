package com.auction.response;

import java.io.Serializable;

public abstract class Response implements Serializable {
    private boolean success;
    private String message;

    public Response(boolean success, String message) {
        this.success = success;
        this.message = message;
    }

    public boolean isSuccess() {
        return success;
    }

    public String getMessage() {
        return message;
    }
}
