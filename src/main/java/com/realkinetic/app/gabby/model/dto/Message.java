package com.realkinetic.app.gabby.model.dto;

import com.realkinetic.app.gabby.util.IdUtil;

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

    private String message;
    private String ackId;
    private String topic;
    private String id;

    public Message() {
        this("", "", "", "");
    }

    public Message(final Message message, final String ackId) {
        this.message = message.message;
        this.topic = message.topic;
        this.id = message.id;
        this.ackId = ackId;
    }

    public Message(final String message, final String ackId, final String topic, final String id) {
        this.message = message;
        this.ackId = ackId;
        this.topic = topic;
        this.id = id;
    }
}
