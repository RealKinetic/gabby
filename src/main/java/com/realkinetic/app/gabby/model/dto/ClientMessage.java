package com.realkinetic.app.gabby.model.dto;

import java.time.LocalDateTime;

public class ClientMessage {
    private Message message;
    private LocalDateTime timestamp;

    public Message getMessage() {
        return message;
    }

    public void setMessage(final Message message) {
        this.message = message;
    }

    public LocalDateTime getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final LocalDateTime timestamp) {
        this.timestamp = timestamp;
    }

    public int getNumAccesses() {
        return numAccesses;
    }

    public void setNumAccesses(final int numAccesses) {
        this.numAccesses = numAccesses;
    }

    private int numAccesses;

    public ClientMessage(final Message message) {
        this.message = message;
        this.timestamp = LocalDateTime.now();
        this.numAccesses = 0;
    }

    public void touch() {
        this.timestamp = LocalDateTime.now();
        this.numAccesses++;
    }
}
