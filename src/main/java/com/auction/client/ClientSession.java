package com.auction.client;

import com.auction.dto.UserSessionDTO;

public class ClientSession {
    private static UserSessionDTO currentUser;

    public static UserSessionDTO getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UserSessionDTO user) {
        currentUser = user;
    }

    public static void clear() {
        currentUser = null;
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }
}