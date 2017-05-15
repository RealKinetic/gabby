package com.realkinetic.app.gabby.repository;

import com.google.common.collect.ImmutableSet;
import io.reactivex.Observable;

import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

import static com.realkinetic.app.gabby.util.IdUtil.generateId;

public class MemorySubscriber implements Subscriber {
    private Map<String, Set<String>> subscribers;

    public MemorySubscriber() {
        this.subscribers = new ConcurrentHashMap<>(10);
    }

    @Override
    public Observable<String> register(String topic) {
        return Observable.defer(() -> {
            String id = generateId();
            this.subscribers.compute(topic, (k, set) -> {
                if (set == null) {
                    set = new ConcurrentSkipListSet<>();
                }
                set.add(id);
                return set;
            });
            return Observable.just(id);
        });
    }

    @Override
    public Observable<Void> deregister(String topic, String subscriptionId) {
        return Observable.defer(() -> {
            this.subscribers.computeIfPresent(topic, (k, set) -> {
                set.remove(subscriptionId);
                if (set.size() == 0) {
                    return null;
                }

                return set;
            });
            return Observable.empty();
        });
    }

    public Observable<ImmutableSet<String>> getSubscribers(String topic) {
        return Observable.defer(() -> {
            Set<String> value = this.subscribers.get(topic);
            if (value == null) {
                value = new HashSet<>();
            }
            return Observable.just(ImmutableSet.<String>builder().addAll(value).build());
        });
    }
}
