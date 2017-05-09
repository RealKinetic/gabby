package com.realkinetic.app.gabby.model;

public class MessageResponse {
    public String getMessage() {
        return message;
    }

    public String getAckId() {
        return ackId;
    }

    final String message;
    final String ackId;

    public MessageResponse(final String message, final String ackId) {
        this.message = message;
        this.ackId = ackId;
    }
}
