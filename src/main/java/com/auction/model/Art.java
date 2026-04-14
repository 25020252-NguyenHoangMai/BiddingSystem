package com.auction.model;

public class Art extends Item {
    private String artist;

    public Art(String id, String name, String description,
               double startingPrice, String artist) {
        super(id, name, description, "ART", startingPrice);
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
