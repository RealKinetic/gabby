package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;

public class StandAloneMessagingService implements MessagingService {
    private final DownstreamSubscription downstreamSubscription;

    @Autowired
    public StandAloneMessagingService(DownstreamSubscription downstreamSubscription) {
        this.downstreamSubscription = downstreamSubscription;
    }

    @Override
    public Observable<String> subscribe(String topic, String subscriptionId) {
        return this.downstreamSubscription.subscribe(topic, subscriptionId);
    }

    @Override
    public Observable<List<String>> unsubscribe(String subscriptionId) {
        return this.downstreamSubscription.unsubscribe(subscriptionId);
    }

    @Override
    public Observable<String> acknowledge(String subscriptionId, Iterable<String> messageIds) {
        return this.downstreamSubscription.acknowledge(subscriptionId, messageIds);
    }

    @Override
    public Observable<List<String>> publish(Message message) {
        return this.downstreamSubscription.publish(message);
    }

    @Override
    public Observable<List<Message>> pull(boolean returnImmediately, String subscriptionId) {
        return this.downstreamSubscription.pull(returnImmediately, subscriptionId).map(messages -> {
            return messages.stream()
                    .map(message -> new Message(message, IdUtil.generateAckId(subscriptionId, message.getId())))
                    .collect(Collectors.toList());
        });
    }
}
