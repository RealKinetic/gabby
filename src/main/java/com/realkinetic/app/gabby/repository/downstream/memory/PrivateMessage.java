package com.realkinetic.app.gabby.repository.downstream.memory;

import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.util.IdUtil;

import java.time.LocalDateTime;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PrivateMessage {
    // if a message has been pulled off the queue 10 times, don't let it happen
    // again
    private static final int MAX_TOUCHES = 10;
    private final Message message;
    private final AtomicInteger numAccesses;
    private final AtomicReference<LocalDateTime> lastAccessed;

    public PrivateMessage(final Message message) {
        this.message = message;
        this.numAccesses = new AtomicInteger(0);
        this.lastAccessed = new AtomicReference<>(LocalDateTime.now());
    }

    public boolean touch() {
        if (this.numAccesses.addAndGet(1) > MAX_TOUCHES) {
            return false;
        }

        this.lastAccessed.set(LocalDateTime.now());
        return true;
    }

    public Message getMessage() {
        return this.message; // given that reading this pointer is only ever done after the initial write this should be threadsafe
    }
}