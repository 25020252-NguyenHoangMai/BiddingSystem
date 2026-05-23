package com.auction.model;

public class OtherItem extends Item {

    private static final long serialVersionUID = 1L;

    public OtherItem() {
        super();
        this.setItemType("OTHER");
    }

    public OtherItem(String id, String name, String imagePath, String sellerId) {
        super(id, name, imagePath, sellerId);
        this.setItemType("OTHER");
    }

    public OtherItem(String id, String name, String description, String imagePath,
                     String itemType, String sellerId, double startingPrice) {
        super(id, name, description, imagePath, itemType, sellerId, startingPrice);
    }


    @Override
    public String getCategoryDetails() {
        return "Category: Other / Uncategorized ( Khác / Chưa được phân loại )";
    }
}