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
package com.realkinetic.app.gabby.model.dto;

public class Message extends ClientMessage {
    public String getAckId() {
        return ackId;
    }

    public String getId() { return id; }

    private String ackId;
    private String id;

    public Message() {}

    public Message(final Message message, final String ackId) {
        this(message.getMessage(), ackId, message.getTopic(), message.id);
    }

    public Message(final String message,
                   final String ackId,
                   final String topic,
                   final String id) {
        super(topic, message);
        this.ackId = ackId;
        this.id = id;
    }
}
