package com.auction.server.factory;

import com.auction.model.Admin;
import com.auction.model.Bidder;
import com.auction.model.User;
import com.auction.server.dto.UserDTO;


public class UserFromDTOFactory {

    public static User fromDTO(UserDTO data) {
        return switch (data.getRole() == null ? "" : data.getRole().toUpperCase()) {
            case "ADMIN" -> new Admin(data.getId(), data.getUsername(), data.getPassword(), data.getFullName());
            case "BIDDER" ->
                    new Bidder(data.getId(), data.getUsername(), data.getPassword(), data.getFullName(), data.getBalance());
            case "SELLER" ->
                    new Seller(data.getId(), data.getUsername(), data.getPassword(), data.getFullName());
            default -> throw new RuntimeException("Role không hợp lệ: " + data.getRole());
        };
    }
}
