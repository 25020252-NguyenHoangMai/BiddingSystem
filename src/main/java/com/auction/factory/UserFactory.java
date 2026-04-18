package com.auction.factory;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dto.UserDTO;


public class UserFactory {

    public static User createUser(UserDTO data) {
        switch (data.role == null ? "" : data.role.toUpperCase()) {
            case "ADMIN":
                return new Admin(data.id, data.username, data.password, data.fullName);
            case "BIDDER":
                return new Bidder(data.id, data.username, data.password, data.fullName, data.balance);
            case "SELLER":
                return new Seller(data.id, data.username, data.password, data.fullName, data.storeName);
            default:
                throw new RuntimeException("Role không hợp lệ: " + data.role);
        }
    }
}
