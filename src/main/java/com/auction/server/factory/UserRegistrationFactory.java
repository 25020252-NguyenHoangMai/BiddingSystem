package com.auction.server.factory;

import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.request.RegisterRequest;


public class UserRegistrationFactory {
    public static User fromRequest(RegisterRequest request) {
        String role = request.getRole().toUpperCase();

        switch (role) {
            case "SELLER":
                Seller seller = new Seller();
                seller.setRole("SELLER");
                return seller;

            case "BIDDER":
                Bidder bidder = new Bidder();
                bidder.setRole("BIDDER");
                return bidder;

            default:
                throw new IllegalArgumentException("Vai trò không hợp lệ!");
        }
    }
}
