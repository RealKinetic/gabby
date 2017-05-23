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

import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.function.Function;

public class MemoryMessage {
    private Message message;
    private long timestamp;
    private int numAccesses;
    private ReadWriteLock lock;

    public Message getMessage() {
        return message;
    }

    public void setMessage(final Message message) {
        this.message = message;
    }

    public long getTimestamp() {
        return this.timestamp;
    }

    public int getNumAccesses() {
        return this.numAccesses;
    }

    public MemoryMessage() {
        this((Message) null);
    }

    public MemoryMessage(final Message message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.numAccesses = 0;
        this.lock = new ReentrantReadWriteLock();
    }

    private <T> T safe(final boolean exclusive, final Callable<T> fn) {
        if (exclusive) {
            this.lock.writeLock().lock();
        } else {
            this.lock.readLock().lock();
        }

        try {
            return fn.call();
        } catch (Exception e) {
            // can't really be thrown
            return null;
        } finally {
            if (exclusive) {
                this.lock.writeLock().unlock();
            } else {
                this.lock.readLock().unlock();
            }
        }
    }

    /*
     * Copy constructor.  Does a shallow copy of Message, which should be
     * immutable.
     */
    public MemoryMessage(final MemoryMessage memoryMessage) {
        this.numAccesses = memoryMessage.numAccesses;
        this.timestamp = memoryMessage.timestamp;
        this.message = memoryMessage.message;
    }

    public void touch() {
        this.timestamp = System.currentTimeMillis();
        this.numAccesses++;
    }
}