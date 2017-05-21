package com.realkinetic.app.gabby.repository;

import com.google.common.collect.ImmutableSet;
import com.realkinetic.app.gabby.model.dto.Message;
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
    private final Subject<Message> observable;
    private final ImmutableSet<String> topics;
    private final Map<String, Disposable> messages;

    public MemoryUpstreamSubscription(Iterable<String> topics) {
        this.observable = PublishSubject.create();
        this.messages = new ConcurrentHashMap<>(10);
        this.topics = ImmutableSet.<String>builder().addAll(topics).build();
    }

    @Override
    public Observable<Message> listen() {
        return this.observable;
    }

    @Override
    public Observable<Void> acknowledge(Iterable<String> ackIds) {
        return Observable.defer(() -> {
            ackIds.forEach(ackId -> {
                this.messages.computeIfPresent(ackId, (k, disposable) -> {
                    disposable.dispose();
                    return null;
                });
            });
            return Observable.empty();
        });
    }

    @Override
    public Observable<Void> push(String topic, String message) {
        return Observable.defer(() -> {
            if (!this.topics.contains(topic)) {
                return Observable.error(
                        new IOException("topic not initialized: " + topic)
                );
            }
            String id = IdUtil.generateId();
            Message mr = new Message(message, id, topic, IdUtil.generateId());
            Disposable disposable =
                    Observable.interval(0, retryTime, timeUnit)
                            .map($ -> mr)
                            .subscribe(this.observable::onNext);
            this.messages.put(id, disposable);
            return Observable.empty();
        });

    }
}
