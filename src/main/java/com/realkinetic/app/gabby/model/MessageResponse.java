package com.realkinetic.app.gabby.model;

public class MessageResponse {
    public String getMessage() {
        return message;
    }

    public String getAckId() {
        return ackId;
    }

    public String getTopic() {
        return topic;
    }

    final String message;
    final String ackId;
    final String topic;

    public MessageResponse(final String message, final String ackId, final String topic) {
        this.message = message;
        this.ackId = ackId;
        this.topic = topic;
    }
}
