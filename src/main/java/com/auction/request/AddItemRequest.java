package com.auction.request;

import com.auction.dto.ItemDTO;

public class AddItemRequest extends Request {
    private final String sellerId;
    private final ItemDTO item;
    private final int durationHours;
    private byte[] imageBytes;
    private String imageFileName;

    public AddItemRequest(String sellerId, ItemDTO item, int durationHours, byte[] imageBytes, String imageFileName) {
        this.sellerId = sellerId;
        this.item = item;
        this.durationHours = durationHours;
        this.imageBytes = imageBytes;
        this.imageFileName = imageFileName;
    }

    public ItemDTO getItem() {
        return item;
    }

    public String getSellerId() {
        return sellerId;
    }

    public int getDurationHours() {
        return  durationHours;
    }

    public byte[] getImageBytes() {
        return imageBytes;
    }

    public String getImageFileName() {
        return imageFileName;
    }

    public void setImageFileName(String imageFileName) {
        this.imageFileName = imageFileName;
    }

    public void setImageBytes(byte[] imageBytes) {
        this.imageBytes = imageBytes;
    }
}
