package com.auction.protocol;

import com.auction.request.Request;

public class RequestMessage implements BaseMessage {
    private final String requestId;
    private final Request payload;

    public RequestMessage(String requestId, Request payload) {
        this.requestId = requestId;
        this.payload = payload;
    }

    public String getRequestId() {
        return requestId;
    }

    public Request getPayload() {
        return payload;
    }
}
