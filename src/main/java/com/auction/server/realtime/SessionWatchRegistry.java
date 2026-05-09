package com.auction.server.realtime;

import com.auction.response.BidUpdateResponse;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class SessionWatchRegistry {
    private final ConcurrentHashMap<String, Set<AuctionSessionObserver>> watchersBySessionId = new ConcurrentHashMap<>();

    public boolean watchSession(String sessionId, AuctionSessionObserver observer) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        if (observer == null) {
            return false;
        }

        watchersBySessionId.computeIfAbsent(sessionId, key -> ConcurrentHashMap.newKeySet()).add(observer);

        return true;
    }

    public boolean unwatchSession(String sessionId, AuctionSessionObserver observer) {
        if (sessionId == null || sessionId.isBlank()) {
            return false;
        }
        if (observer == null) {
            return false;
        }

        Set<AuctionSessionObserver> observers = watchersBySessionId.get(sessionId);
        if (observers == null) {
            return false;
        }

        boolean removed = observers.remove(observer); //observers là 1 set, method remove() của set trả boolean
        if (observers.isEmpty()) {
            watchersBySessionId.remove(sessionId, observers); //thread-safe, remove có đk (compare and remove)
        }

        return removed;
    }

    public void unwatchAll(AuctionSessionObserver observer) {
        if (observer == null) {
            return; //dừng và thoát khỏi method
        }

//        Cách 1:
//        for (String sessionId : watchersBySessionId.keySet()) {
//            Set<AuctionSessionObserver> observers = watchersBySessionId.get(sessionId);
//            if (observers == null) {
//                continue;
//            }
//            observers.remove(observer);
//            if (observers.isEmpty()) {
//                watchersBySessionId.remove(sessionId, observers);
//            }
//        }
//        Cách 2: dùng entrySet()
        for (Map.Entry<String, Set<AuctionSessionObserver>> entry : watchersBySessionId.entrySet()) {
            String sessionId = entry.getKey();
            Set<AuctionSessionObserver> observers = entry.getValue();

            observers.remove(observer);
            if (observers.isEmpty()) {
                watchersBySessionId.remove(sessionId, observers);
            }
        }
    }

    public int broadcastBidUpdate(String sessionId, BidUpdateResponse update) { //trả về int (số observer gửi thành công,dễ debug)
        if (sessionId == null || sessionId.isBlank()) {
            return 0;
        }
        if (update == null) {
            return 0;
        }

        Set<AuctionSessionObserver> observers = watchersBySessionId.get(sessionId);
        if (observers == null || observers.isEmpty()) {
            return 0;
        }
        if (observers.isEmpty()) {
            watchersBySessionId.remove(sessionId, observers);
        }

        List<AuctionSessionObserver> observersList = new ArrayList<>(observers); // tạo list để khi sửa thì ko sửa trực tiếp set
        int successCount = 0;
        for (AuctionSessionObserver observer : observersList) {
            if (observer == null) { //ds có observer null thì skip, sang observer tiếp (để tránh nullpointer)
                continue;
            }

            try {
                boolean success = observer.onBidUpdated(update);
                if (success) {
                    successCount++;
                } else { //false: có thể socket lỗi hoặc client disconnect
                    unwatchAll(observer);
                }
            } catch (Exception e) {
                unwatchAll(observer);
            }
        }
        if (observers.isEmpty()) {
            watchersBySessionId.remove(sessionId, observers);
        }

        return successCount;
    }
}
