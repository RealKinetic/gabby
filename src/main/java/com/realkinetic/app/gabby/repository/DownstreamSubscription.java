package com.realkinetic.app.gabby.repository;

import com.realkinetic.app.gabby.model.dto.Message;
import io.reactivex.Maybe;
import io.reactivex.Observable;

import java.util.List;

public interface DownstreamSubscription {
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
    Observable<Maybe<Message>> pull(String subscriptionId);
}
