package com.auction.response;

import com.auction.dto.BidHistoryEntryDTO;

import java.util.List;

public class GetBidHistoryResponse extends Response {
    private final List<BidHistoryEntryDTO> history;

    public GetBidHistoryResponse(boolean success, String message, List<BidHistoryEntryDTO> history) {
        super(success, message);
        this.history = history;
    }

    public List<BidHistoryEntryDTO> getHistory() {
        return history;
    }
}
