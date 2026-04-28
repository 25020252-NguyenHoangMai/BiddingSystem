package com.auction.server.service;

import com.auction.model.AuctionSession;

import java.time.Duration;
import java.time.LocalDateTime;

public class AntiSnipingService {
    private static final Duration antisnipingThreshold = Duration.ofSeconds(30);
    private static final Duration extendTime = Duration.ofSeconds(60);

    public AntiSnipingService() {}

    public boolean shouldExtend(AuctionSession session) {
        if (session == null) {
            return false;
        }
        if (session.getEndTime() == null) {
            return false;
        }
        if (!SessionService.STATUS_RUNNING.equals(session.getStatus())) {
            return false;
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime endTime = session.getEndTime();

        if (!now.isBefore(endTime)) {
            return false;
        }

        Duration remainingTime = Duration.between(now, endTime);
        return remainingTime.compareTo(antisnipingThreshold) <= 0;
    }

    public Duration getExtendTime() {
        return extendTime;
    }

    public Duration getAntisnipingThreshold() {
        return antisnipingThreshold;
    }
}
