package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.MessageResponse;

import java.io.IOException;

public interface MessagingService {
    String createSubscription(String topic) throws IOException;
    void deleteSubscription(String subscriptionName) throws IOException;
    Iterable<MessageResponse> pull(String subscriptionName) throws IOException;
    void acknowledge(String subscriptionName, Iterable<String> ackIds) throws IOException;
    void send(String topic, String message) throws IOException;
}
