package com.auction.server.factory;

import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.request.RegisterRequest;


public class UserRegistrationFactory {
    public static User fromRequest(RegisterRequest request) {
        Bidder bidder = new Bidder();

        bidder.setRole("BIDDER");
        bidder.setBalance(0.0);
        bidder.setSellerEnabled(false);

        return bidder;
    }
}
