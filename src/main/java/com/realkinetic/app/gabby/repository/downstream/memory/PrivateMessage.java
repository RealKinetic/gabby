/*
Copyright 2017 Real Kinetic LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
*/
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