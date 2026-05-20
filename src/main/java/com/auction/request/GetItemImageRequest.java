package com.auction.request;

public class GetItemImageRequest extends Request {
    private String imagePath;

    public GetItemImageRequest(String imagePath) {
        this.imagePath = imagePath;
    }

    public String getImagePath() {
        return imagePath;
    }
}
