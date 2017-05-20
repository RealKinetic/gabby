package com.realkinetic.app.gabby.model.dto;

import java.time.LocalDateTime;

public class ClientMessage {
    private Message message;
    private long timestamp;

    public Message getMessage() {
        return message;
    }

    public void setMessage(final Message message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(final long timestamp) {
        this.timestamp = timestamp;
    }

    public int getNumAccesses() {
        return numAccesses;
    }

    public void setNumAccesses(final int numAccesses) {
        this.numAccesses = numAccesses;
    }

    private int numAccesses;

    public ClientMessage() {
        this(null);
    }

    public ClientMessage(final Message message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.numAccesses = 0;
    }

    public void touch() {
        this.timestamp = System.currentTimeMillis();
        this.numAccesses++;
    }
}
