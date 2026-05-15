package com.auction.client.service;

import com.auction.client.ClientSession;
import com.auction.client.service.AuctionService;
import com.auction.dto.ItemDTO;
import com.auction.dto.UserSessionDTO;
import com.auction.response.PlaceBidResponse;
import javafx.application.Platform;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;

public class AutoBidService {
    private final AuctionService auctionService;
    private final AtomicBoolean running = new AtomicBoolean(false);

    public AutoBidService(AuctionService auctionService) {
        this.auctionService = auctionService;
    }

    public boolean isRunning() {
        return running.get();
    }

    public void stop() {
        running.set(false);
    }

    public void start(
            ItemDTO currentItem,
            Runnable onStarted,
            Runnable onStopped,
            Consumer<PlaceBidResponse> onBidSuccess,
            Consumer<String> onError
    ) {
        if (running.get()) {
            stop();

            Platform.runLater(onStopped);
            return;
        }

        running.set(true);

        Platform.runLater(onStarted);

        Thread thread = new Thread(() -> {
            try {
                while (running.get()) {
                    if (currentItem == null) {
                        break;
                    }

                    if (isClosed(currentItem.getSessionStatus())) {
                        break;
                    }

                    UserSessionDTO currentUser = ClientSession.getCurrentUser();

                    if (currentUser == null) {
                        break;
                    }

                    Double minBid = currentItem.getMinimumNextBid();

                    if (minBid == null) {
                        Thread.sleep(1000);
                        continue;
                    }

                    double available =
                            currentUser.getBalance()
                                    - currentUser.getReservedBalance();

                    if (available < minBid) {

                        Platform.runLater(() ->
                                onError.accept("Not enough available balance.")
                        );

                        break;
                    }

                    try {

                        PlaceBidResponse res = auctionService.placeBid(
                                currentItem.getSessionId(),
                                currentUser.getId(),
                                minBid
                        );

                        if (!res.isSuccess()) {
                            Thread.sleep(1200);
                            continue;
                        }

                        if (res.getUpdatedUser() != null) {
                            ClientSession.setCurrentUser(res.getUpdatedUser());
                        }

                        Platform.runLater(() ->
                                onBidSuccess.accept(res)
                        );

                        Thread.sleep(1200);

                    } catch (Exception ex) {

                        ex.printStackTrace();

                        Thread.sleep(1500);
                    }
                }

            } catch (Exception ex) {

                ex.printStackTrace();

            } finally {

                running.set(false);

                Platform.runLater(onStopped);
            }

        });

        thread.setDaemon(true);
        thread.start();
    }

    private boolean isClosed(String status) {
        return "FINISHED".equals(status)
                || "CANCELED".equals(status)
                || "PAID".equals(status);
    }
}
