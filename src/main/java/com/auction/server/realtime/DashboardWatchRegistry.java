package com.auction.server.realtime;

import com.auction.response.DashboardUpdateResponse;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class DashboardWatchRegistry {
    private final Set<DashboardObserver> observers = ConcurrentHashMap.newKeySet();

    public boolean watchDashboard(DashboardObserver observer) {
        if (observer == null) return false;
        observers.add(observer);
        System.out.println("[DashboardWatchRegistry] Dashboard watcher added. Total = " + observers.size());
        return true;
    }

    public boolean unwatchDashboard(DashboardObserver observer) {
        if (observer == null) return false;
        boolean removed = observers.remove(observer);

        System.out.println("[DashboardWatchRegistry] Dashboard watcher removed. Total = " + observers.size());

        return removed;
    }

    public void unwatchAll(DashboardObserver observer) {
        if (observer != null) {
            observers.remove(observer);
            System.out.println("[DashboardWatchRegistry] Dashboard watcher removed by disconnect. Total = " + observers.size());
        }
    }

    public int broadcastDashboardUpdate(DashboardUpdateResponse update) {
        int count = 0;
        System.out.println("[DashboardWatchRegistry] Broadcasting dashboard update to " + observers.size() + " watchers");

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
        System.out.println("[DashboardWatchRegistry] Broadcast success = " + count);

        return count;
    }
}
