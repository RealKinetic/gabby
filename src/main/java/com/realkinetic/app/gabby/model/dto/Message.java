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
