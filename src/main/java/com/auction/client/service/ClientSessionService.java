package com.auction.client.service;

import com.auction.client.service.UserClientService;
import com.auction.client.ClientSession;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.response.BidUpdateResponse;
import javafx.concurrent.Task;

import java.util.Objects;
import java.util.function.Consumer;

public class ClientSessionService {
    private final UserClientService userClientService =
            UserClientService.getInstance();

    public void refreshCurrentUserIfAffected(
            ItemDTO currentItem,
            BidUpdateResponse update,
            Runnable onSuccess,
            Consumer<Throwable> onError
    ) {

        UserSessionDTO currentUser = ClientSession.getCurrentUser();

        if (currentUser == null || currentItem == null || update == null) {
            return;
        }

        boolean isWinner = Objects.equals(
                        currentUser.getId(),
                        update.getCurrentWinnerId()
                );

        boolean isSeller = Objects.equals(
                        currentUser.getId(),
                        currentItem.getSellerId()
                );

        if (!isWinner && !isSeller) {
            return;
        }

        Task<UserSessionDTO> task = new Task<>() {
            @Override
            protected UserSessionDTO call() throws Exception {
                return userClientService.getCurrentUser(currentUser.getId());
            }
        };

        task.setOnSucceeded(e -> {
            ClientSession.setCurrentUser(task.getValue());

            if (onSuccess != null) {
                onSuccess.run();
            }
        });

        task.setOnFailed(e -> {
            if (onError != null) {
                onError.accept(task.getException());
            }
        });

        Thread thread = new Thread(task);
        thread.setDaemon(true);
        thread.start();
    }
}
