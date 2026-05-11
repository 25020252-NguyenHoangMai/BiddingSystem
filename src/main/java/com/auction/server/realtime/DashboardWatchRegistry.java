package com.auction.server.realtime;

import com.auction.response.DashboardUpdateResponse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DashboardWatchRegistry {
    private final Set<DashboardObserver> observers = ConcurrentHashMap.newKeySet();

    public boolean watchDashboard(DashboardObserver observer) {
        if (observer == null) return false;
        observers.add(observer);
        return true;
    }

    public boolean unwatchDashboard(DashboardObserver observer) {
        if (observer == null) return false;
        return observers.remove(observer);
    }

    public void unwatchAll(DashboardObserver observer) {
        if (observer != null) {
            observers.remove(observer);
        }
    }

    public int broadcastDashboardUpdate(DashboardUpdateResponse update) {
        int count = 0;

        for (DashboardObserver observer : observers) {
            try {
                if (observer.onDashboardUpdate(update)) {
                    count++;
                } else {
                    observers.remove(observer);
                }
            } catch (Exception e) {
                observers.remove(observer);
            }
        }

        return count;
    }
}
