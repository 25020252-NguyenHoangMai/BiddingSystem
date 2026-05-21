package com.auction.client.service;

import com.auction.client.ClientSession;
import com.auction.client.network.ClientSocket;
import com.auction.request.*;
import com.auction.response.*;

public class AuctionService {
    private final ClientSocket socket = ClientSocket.getInstance();

    // ===== PLACE BID =====
    public PlaceBidResponse placeBid(String sessionId, String bidderId, double amount) throws Exception {
        return socket.sendRequestAndWait(
                new PlaceBidRequest(sessionId, bidderId, amount), PlaceBidResponse.class
        );
    }

    public SessionWatchResponse watchSession(String sessionId, String userId) throws Exception {
        return socket.sendRequestAndWait(
                new WatchSessionRequest(sessionId, userId), SessionWatchResponse.class);
    }

    public GetBidHistoryResponse getBidHistory(String sessionId) throws Exception {
        return socket.sendRequestAndWait(
                new GetBidHistoryRequest(sessionId), GetBidHistoryResponse.class);
    }

    public GetAuctionDetailResponse getAuctionDetail(String sessionId) throws Exception {
        return socket.sendRequestAndWait(
                new GetAuctionDetailRequest(sessionId),
                GetAuctionDetailResponse.class
        );
    }
    public GetSessionHistoryResponse getSessionHistory(String userId) throws Exception {
        return socket.sendRequestAndWait(
                new GetSessionHistoryRequest(userId),
                GetSessionHistoryResponse.class
        );
    }
    public GetSellerHistoryResponse getSellerHistory(String userId) throws Exception {
        return socket.sendRequestAndWait(
                new GetSellerHistoryRequest(userId),
                GetSellerHistoryResponse.class
        );
    }

    public void unwatchSession(String sessionId) {
        try {
            if (!socket.isConnectedPublic()) {
                return;
            }

            socket.sendRequest(new UnwatchSessionRequest(sessionId));
        } catch (Exception ignored) {}
    }

    public SetAutoBidResponse setAutoBid(String sessionId, String bidderId, double maxAmount) throws Exception {
        return socket.sendRequestAndWait(
                new SetAutoBidRequest(sessionId, bidderId, maxAmount), SetAutoBidResponse.class);
    }

    public void closeAllSockets() {
        try {
            socket.close();
        } catch (Exception ignored) {}
    }
}
