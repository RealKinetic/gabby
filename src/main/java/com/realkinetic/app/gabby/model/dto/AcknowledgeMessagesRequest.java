package com.realkinetic.app.gabby.model.dto;

import java.util.List;

public class AcknowledgeMessagesRequest {
    public List<String> getAckIds() {
        return ackIds;
    }

    public void setAckIds(List<String> ackIds) {
        this.ackIds = ackIds;
    }

    private List<String> ackIds;
}
