package com.realkinetic.app.gabby.model.dto;

public class ClientMessage {
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    private String message;
    private String topic;

    public ClientMessage() {}

    public ClientMessage(final String topic, final String message) {
        this.topic = topic;
        this.message = message;
    }
}
