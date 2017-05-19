package com.realkinetic.app.gabby.model.dto;

public class Message {
    public String getMessage() {
        return message;
    }

    public String getAckId() {
        return ackId;
    }

    public String getTopic() {
        return topic;
    }

    public String getId() { return id; }

    final String message;
    final String ackId;
    final String topic;
    final String id;

    public Message(final String message, final String ackId, final String topic, final String id) {
        this.message = message;
        this.ackId = ackId;
        this.topic = topic;
        this.id = id;
    }
}
