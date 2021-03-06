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
package com.realkinetic.app.gabby.repository.downstream.redis;

import com.realkinetic.app.gabby.model.dto.Message;

public class RedisMessage {
    private Message message;
    private long timestamp;
    private int numAccesses;

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

    public RedisMessage() {
        this((Message) null);
    }

    public RedisMessage(final Message message) {
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.numAccesses = 0;
    }

    /*
     * Copy constructor.  Does a shallow copy of Message, which should be
     * immutable.
     */
    public RedisMessage(final RedisMessage redisMessage) {
        this.numAccesses = redisMessage.numAccesses;
        this.timestamp = redisMessage.timestamp;
        this.message = redisMessage.message;
    }

    public void touch() {
        this.timestamp = System.currentTimeMillis();
        this.numAccesses++;
    }
}
