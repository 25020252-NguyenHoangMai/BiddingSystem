package com.auction.model;

public class Art extends Item {

    private static final long serialVersionUID = 1L;

    private String artist;

    public Art(){}

    public Art(String id, String name, String description, String sellerID, double startingPrice, String artist) {
        super(id, name, description, "ART", sellerID, startingPrice);
        this.artist = artist;
    }

    @Override
    public String getCategoryDetails() {
        return "Nghệ thuật - Tác giả: " + artist;
    }

    public String getArtist() {
        return artist;
    }

    public void setArtist(String artist) {
        this.artist = artist;
    }
}
