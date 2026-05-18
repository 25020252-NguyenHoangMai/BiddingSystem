package com.auction.protocol;

public class EventMessage implements BaseMessage {
    private final Object payload;

    public EventMessage(Object payload) {
        this.payload = payload;
    }

    public Object getPayload() {
        return payload;
    }

}
