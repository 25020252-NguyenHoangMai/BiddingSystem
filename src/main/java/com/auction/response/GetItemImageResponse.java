package com.auction.response;

public class GetItemImageResponse extends Response {
    private final byte[] imageBytes;

    public GetItemImageResponse(boolean success, String message, byte[] imageBytes) {
        super(success, message);
        this.imageBytes = imageBytes;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }
}
