package com.realkinetic.app.gabby.repository;

import com.google.common.collect.ImmutableSet;
import com.realkinetic.app.gabby.model.MessageResponse;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class MemoryUpstreamSubscription implements UpstreamSubscription {
    private static Logger log = Logger.getLogger(MemoryUpstreamSubscription.class.getName());
    public static int retryTime = 10;
    public static TimeUnit timeUnit = TimeUnit.SECONDS; // emit every 10 seconds
    private final Subject<MessageResponse> observable;
    private final ImmutableSet<String> topics;
    private final Map<String, Disposable> messages;

    public MemoryUpstreamSubscription(Iterable<String> topics) {
        this.observable = PublishSubject.create();
        this.messages = new ConcurrentHashMap<>(10);
        this.topics = ImmutableSet.<String>builder().addAll(topics).build();
    }

    @Override
    public Observable<MessageResponse> listen() {
        return this.observable;
    }

    @Override
    public void acknowledge(Iterable<String> ackIds) throws IOException {
        ackIds.forEach(ackId -> {
            this.messages.computeIfPresent(ackId, (k, disposable) -> {
                disposable.dispose();
                return null;
            });
        });
    }

    @Override
    public void push(String topic, String message) throws IOException {
        if (!this.topics.contains(topic)) {
            throw new IOException("topic not initialized: " + topic);
        }

        String id = IdUtil.generateId();
        MessageResponse mr = new MessageResponse(message, id, topic);
        Disposable disposable =
                Observable.interval(0, retryTime, timeUnit)
                    .map($ -> mr)
                    .subscribe(this.observable::onNext);
        this.messages.put(id, disposable);
    }
}
