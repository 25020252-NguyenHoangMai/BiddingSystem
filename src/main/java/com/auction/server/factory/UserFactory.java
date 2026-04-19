package com.auction.server.factory;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.Seller;
import com.auction.model.User;
import com.auction.server.dto.UserDTO;


public class UserFactory {

    public static User createUser(UserDTO data) {
        switch (data.getRole() == null ? "" : data.getRole().toUpperCase()) {
            case "ADMIN":
                return new Admin(data.getId(), data.getUsername(), data.getPassword(), data.getFullName());
            case "BIDDER":
                return new Bidder(data.getId(), data.getUsername(), data.getPassword(), data.getFullName(), data.getBalance());
            case "SELLER":
                return new Seller(data.getId(), data.getUsername(), data.getPassword(), data.getFullName(), data.getStoreName());
            default:
                throw new RuntimeException("Role không hợp lệ: " + data.getRole());
        }
    }
}
