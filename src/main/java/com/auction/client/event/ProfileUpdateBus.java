package com.auction.client.event;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.Consumer;

public class ProfileUpdateBus {

    private static final List<Consumer<String>> listeners = new CopyOnWriteArrayList<>();

    public static void subscribe(Consumer<String> listener) {
        listeners.add(listener);
    }

    public static void publishUsernameChanged(String userId, String newUsername) {
        String payload = userId + "|" + newUsername;

        for (Consumer<String> listener : listeners) {
            listener.accept(payload);
        }
    }
}
