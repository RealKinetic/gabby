package com.realkinetic.app.gabby.repository.downstream.memory;

import com.google.common.collect.ImmutableSet;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.model.error.*;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.util.*;
import java.util.concurrent.ConcurrentSkipListSet;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.stream.Collectors;

public class MemoryDownstreamSubscription implements DownstreamSubscription {
    private final ReadWriteLock lock;
    // key for subscribers is topic id
    private final Map<String, Set<String>> topics;
    private final Map<String, MessageBroker> subscriptions;

    public MemoryDownstreamSubscription() {
        this.topics = new HashMap<>(10);
        this.subscriptions = new HashMap<>(10);
        this.lock = new ReentrantReadWriteLock();
    }

    @Override
    public Observable<String> subscribe(String topic, String subscriptionId) {
        return Observable.defer(() -> {
            this.lock.writeLock().lock();
            try {
                this.topics.compute(topic, (k, set) -> {
                    if (set == null) {
                        set = new HashSet<>(10);
                    }
                    set.add(subscriptionId);
                    MessageBroker mb = new MessageBroker(topic, subscriptionId);
                    this.subscriptions.put(subscriptionId, mb);
                    return set;
                });

                this.lock.writeLock().unlock();
                return Observable.just(subscriptionId);
            } finally {
                this.lock.writeLock().unlock();
            }
        }).subscribeOn(Schedulers.computation());
    }

    @Override
    public Observable<List<String>> unsubscribe(String subscriptionId) {
        return Observable.defer(() -> {
            this.lock.writeLock().lock();
            // unfortunately, we are in this lock a bit longer than we want to be
            try {
                MessageBroker mb = this.subscriptions.get(subscriptionId);
                if (mb == null) {
                    return Observable.error(new InvalidSubscriptionException(subscriptionId));
                }
                this.subscriptions.remove(subscriptionId);
                Set<String> subIds = this.topics.get(mb.getTopic());
                if (subIds != null) {
                    subIds.remove(subscriptionId);
                }

                return Observable.just(mb.dispose());
            } finally {
                this.lock.writeLock().unlock();
            }
        }).subscribeOn(Schedulers.computation());
    }

    @Override
    public Observable<String> acknowledge(final String subscriptionId, final Iterable<String> messageIds) {
        return Observable.defer(() -> {
            final MessageBroker mb;
            this.lock.readLock().lock();
            try {
                mb = this.subscriptions.get(subscriptionId);
            } finally {
                this.lock.readLock().unlock();
            }

            if (mb == null) {
                return Observable.error(new InvalidSubscriptionException(subscriptionId));
            }

            messageIds.forEach(mb::acknowledge);

            return Observable.just(subscriptionId);
        }).subscribeOn(Schedulers.computation());
    }

    @Override
    public Observable<List<String>> publish(Message message) {
        return Observable.defer(() -> {
            this.lock.readLock().lock();
            // we're going to pull brokers out into this linked list to get
            // out of the read lock as soon as possible
            final LinkedList<MessageBroker> brokers = new LinkedList<>();
            try {
                this.topics.computeIfPresent(message.getTopic(), ($, set) -> {
                    set.forEach(subscriptionId -> {
                        MessageBroker mb = this.subscriptions.get(subscriptionId);
                        if (mb == null) { // incorrect state, can't correct here because we're only in a read lock
                            return;
                        }
                        brokers.add(mb);
                    });

                    return set;
                });
            } finally {
                this.lock.readLock().unlock();
            }

            return Observable.just(brokers
                    .stream()
                    .map(mb -> {
                        mb.push(message);
                        return mb.getSubscriptionId();
                    }).collect(Collectors.toList())
            );
        }).subscribeOn(Schedulers.computation());
    }

    @Override
    public Observable<List<Message>> pull(final boolean returnImmediately, String subscriptionId) {
        return Observable.defer(() -> {
            this.lock.readLock().lock();
            final MessageBroker mb;
            // get out of the lock as soon as possible
            try{
                mb = this.subscriptions.get(subscriptionId);
            } finally {
                this.lock.readLock().unlock();
            }

            if (mb == null) {
                return Observable.error(new InvalidSubscriptionException(subscriptionId));
            }

            return mb.pull();
        }).subscribeOn(Schedulers.computation());
    }

    public Observable<ImmutableSet<String>> getSubscribers(String topic) {
        return Observable.defer(() -> {
            Set<String> value = this.topics.get(topic);
            if (value == null) {
                value = new ConcurrentSkipListSet<>();
            }
            return Observable.just(ImmutableSet.<String>builder().addAll(value).build());
        }).subscribeOn(Schedulers.computation());
    }
}
