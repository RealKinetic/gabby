package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.MessageResponse;
import com.realkinetic.app.gabby.model.dto.CreateSubscriptionRequest;
import io.reactivex.Observable;

import java.io.IOException;

public interface MessagingService {
    String createSubscription(CreateSubscriptionRequest request) throws IOException;
    void deleteSubscription(String subscriptionName) throws IOException;
    Observable<Iterable<MessageResponse>> pull(String subscriptionName) throws IOException;
    void acknowledge(String subscriptionName, Iterable<String> ackIds) throws IOException;
    void send(String topic, String message) throws IOException;
}
