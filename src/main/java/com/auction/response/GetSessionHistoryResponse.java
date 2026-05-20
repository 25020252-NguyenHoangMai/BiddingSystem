package com.auction.response;

import com.auction.dto.SessionHistoryItemDTO;
import java.util.List;

public class GetSessionHistoryResponse extends Response {
    private final List<SessionHistoryItemDTO> sessions;

    public GetSessionHistoryResponse(boolean success, String message, List<SessionHistoryItemDTO> sessions) {
        super(success, message);
        this.sessions = sessions;
    }

    public List<SessionHistoryItemDTO> getSessions() {
        return sessions;
    }
}