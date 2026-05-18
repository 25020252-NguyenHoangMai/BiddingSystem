package com.auction.protocol;

import com.auction.response.Response;

public class ResponseMessage implements BaseMessage {
    private final String requestId;
    private final Response payload;

    public ResponseMessage(String requestId, Response payload) {
        this.requestId = requestId;
        this.payload = payload;
    }

    public String getRequestId() {
        return requestId;
    }

    public Response getPayload() {
        return payload;
    }
}
