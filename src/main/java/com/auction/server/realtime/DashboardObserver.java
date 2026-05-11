package com.auction.server.realtime;

import com.auction.response.DashboardUpdateResponse;

public interface DashboardObserver {
    boolean onDashboardUpdate(DashboardUpdateResponse update);
}
