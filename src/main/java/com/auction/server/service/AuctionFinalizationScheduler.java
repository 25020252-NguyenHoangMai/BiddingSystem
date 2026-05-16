package com.auction.server.service;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AuctionFinalizationScheduler {
    private final SessionService sessionService;
    private final ScheduledExecutorService executor =
            Executors.newSingleThreadScheduledExecutor(r -> {
                Thread thread = new Thread(r, "AuctionFinalizationScheduler");
                thread.setDaemon(true);
                return thread;
            });

    public AuctionFinalizationScheduler(SessionService sessionService) {
        this.sessionService = sessionService;
    }

    public void start() {
        executor.scheduleAtFixedRate(
                this::runOnce,
                0,
                5,
                TimeUnit.SECONDS
        );
    }

    private void runOnce() {
        try {
            sessionService.finalizeExpiredSessions();
        } catch (Exception e) {
            System.err.println("[AuctionFinalizationScheduler] Failed to finalize expired sessions");
            e.printStackTrace();
        }
    }

    public void stop() {
        executor.shutdownNow();
    }
}
