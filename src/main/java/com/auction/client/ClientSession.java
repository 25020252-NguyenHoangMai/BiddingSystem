package com.auction.client;

import com.auction.dto.UserSessionDTO;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ClientSession {
    private static UserSessionDTO currentUser;
    private static final List<Consumer<UserSessionDTO>> listeners = new CopyOnWriteArrayList<>();

    public static UserSessionDTO getCurrentUser() {
        return currentUser;
    }

    public static void setCurrentUser(UserSessionDTO user) {
        currentUser = user;
        notifyListeners();

    }

    public static void clear() {
        currentUser = null;
        notifyListeners();
    }

    public static boolean isLoggedIn() {
        return currentUser != null;
    }

    public static void addUserChangeListener(Consumer<UserSessionDTO> listener) {
        listeners.add(listener);
    }

    public static void removeUserChangeListener(Consumer<UserSessionDTO> listener) {
        listeners.remove(listener);
    }

    private static void notifyListeners() {
        for (Consumer<UserSessionDTO> listener : listeners) {
            listener.accept(currentUser);
        }
    }

}