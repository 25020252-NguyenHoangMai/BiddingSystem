//package com.auction.server.factory;
//
//import com.auction.model.Admin;
//import com.auction.model.Bidder;
//import com.auction.model.User;
//import com.auction.dto.UserDTO;
//
//
//public class UserFromDTOFactory {
//
//    public static User fromDTO(UserDTO data) {
//        return switch (data.getRole() == null ? "" : data.getRole().toUpperCase()) {
//            case "ADMIN" -> new Admin(data.getId(), data.getUsername(), data.getPassword(), data.getFullName());
//            case "BIDDER" -> {
//                Bidder bidder = new Bidder(data.getId(), data.getUsername(), data.getPassword(), data.getFullName(), data.getRole(), data.getAvailableBalance());
//                bidder.setSellerEnabled(data.isSellerEnabled());
//
//                yield bidder;
//            }
//            default -> throw new RuntimeException("Role không hợp lệ: " + data.getRole());
//        };
//    }
//}
