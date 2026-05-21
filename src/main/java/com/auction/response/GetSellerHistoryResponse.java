package com.auction.response;

import com.auction.dto.SellerHistoryItemDTO;
import com.auction.dto.SessionHistoryItemDTO;
import java.util.List;

public class GetSellerHistoryResponse extends Response {
    private final List<SellerHistoryItemDTO> sessions;

    public GetSellerHistoryResponse(boolean success, String message, List<SellerHistoryItemDTO> sessions) {
        super(success, message);
        this.sessions = sessions;
    }

    public List<SellerHistoryItemDTO> getSessions() {
        return sessions;
    }
}