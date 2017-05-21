package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.model.dto.CreateSubscriptionRequest;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.List;

public interface MessagingService {
    // returns the subscription id
    Observable<String> subscribe(String topic, String subscriptionId);
    // returns a list of message ids that this subscription has not yet
    // acknowledged.
    Observable<List<String>> unsubscribe(String subscriptionId);
    // returns the subscriptionid
    Observable<String> acknowledge(String subscriptionId, Iterable<String> messageIds);
    // returns a list of subscriptionIds that were "notified" of the publish
    Observable<List<String>> publish(Message message);
    // this will be null in the case of a timeout, force client to resubscribe
    Observable<List<Message>> pull(boolean returnImmediately, String subscriptionId);
}
