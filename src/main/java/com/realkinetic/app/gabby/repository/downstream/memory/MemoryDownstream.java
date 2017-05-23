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
package com.realkinetic.app.gabby.repository.downstream.memory;

import com.google.common.collect.Sets;
import com.realkinetic.app.gabby.config.Config;
import com.realkinetic.app.gabby.config.MemoryConfig;
import com.realkinetic.app.gabby.model.dto.ClientMessage;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Predicate;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MemoryDownstream implements DownstreamSubscription {
    private static final Logger LOG = Logger.getLogger(MemoryDownstream.class.getName());
    private final Config config;
    private final MemoryConfig memoryConfig;
    private final Map<String, Set<String>> sets;
    private final Map<String, BlockingQueue<MemoryMessage>> queues;
    private final Map<String, PriorityBlockingQueue<MemoryMessage>> deadLetterQueues;
    // this is a hack to assist unit testing... we need a way to tell this
    // logic to evict an item from the deadletter queue even though that is
    // based on timestamps by default.
    private final Predicate<MemoryMessage> predicate;

    @Autowired
    public MemoryDownstream(final Config config) {
        this(config, mm ->
            mm.getTimestamp() < System.currentTimeMillis() - config.getDownstreamTimeout() * 1000
        );
    }

    public MemoryDownstream(Config config, final Predicate<MemoryMessage> predicate) {
        this.config = config;
        this.memoryConfig = config.getMemoryConfig();
        this.sets = new ConcurrentSkipListMap<>();
        this.queues = new ConcurrentSkipListMap<>();
        this.deadLetterQueues = new ConcurrentSkipListMap<>();
        this.predicate = predicate;

        Observable.interval(
                config.getDownstreamTimeout(),
                config.getDownstreamTimeout(),
                TimeUnit.SECONDS
        ).retry(10).subscribe($ -> this.vacuum(), err -> {
            LOG.severe(err.getMessage());
        });
    }

    private void vacuum() {
        final List<MemoryMessage> revived = new ArrayList<>(10);
        final AtomicInteger i = new AtomicInteger(0); // tells us how many to take
        // what we need to do is determine how many items from the front of the queue
        // we need to pull... this doesn't guarantee we get them all, but this should
        // mostly clear itself up on the next volume
        this.deadLetterQueues.forEach((subscriptionId, deadLetterQueue) -> {
            for (MemoryMessage message : deadLetterQueue) {
                if (this.predicate.test(message)) {
                    i.incrementAndGet();
                } else {
                    break;
                }
            }

            if (i.get() > 0) { // we need to move some items over the queue
                // first, collect them
                deadLetterQueue.drainTo(revived, i.get());
                final BlockingQueue<MemoryMessage> queue = this.getQueue(subscriptionId);
                // we do this because it is way more efficient to add items to the concurrent
                // queue all at once
                final LinkedList<MemoryMessage> toAdd = new LinkedList<>();
                // a list of yo-yoing messages, we're going to just acknowlege
                final LinkedList<String> toRemove = new LinkedList<>();

                for (MemoryMessage mm : revived) {
                    if (mm.getNumAccesses() < this.memoryConfig.getMaxAccesses() - 1) {
                        mm.touch();
                        toAdd.addFirst(mm);
                    } else {
                        toRemove.add(mm.getMessage().getId());
                    }
                }

                queue.addAll(toAdd);

                if (toRemove.size() > 0) {
                    this.acknowledge(subscriptionId, toRemove).subscribe();
                }
            }

            i.set(0); // reset i to 0
            revived.clear(); // remove old elements
        });
    }

    private Set<String> getSet(String key) {
        return this.sets.computeIfAbsent(key, k -> new ConcurrentSkipListSet<>());
    }

    private BlockingQueue<MemoryMessage> getQueue(String key) {
        return this.queues.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());
    }

    private PriorityBlockingQueue<MemoryMessage> getDeadLetterQueue(String key) {
        return this.deadLetterQueues.computeIfAbsent(key, k -> new PriorityBlockingQueue<MemoryMessage>(10000, (m1, m2) -> {
            if (m1.getTimestamp() < m2.getTimestamp()) {
                return -1;
            } else if (m2.getTimestamp() < m1.getTimestamp()) {
                return 1;
            }
            return 0;
        }));
    }

    private void deleteSubscription(final String subscriptionId) {
        this.sets.remove(subscriptionId);
        this.queues.remove(subscriptionId);
        this.deadLetterQueues.remove(subscriptionId);
    }

    @Override
    public Observable<String> subscribe(String topic, String subscriptionId) {
        return Observable.defer(() -> {
            // we'll do this in a very specific order since it isn't atomic
            // we only need the topics set for the unsubscribe side so we'll
            // write that first.  No other operation will be impacted until
            // the subscribers set is written.  Going in the other direction,
            // (unsubscribe), we the topics set first.  If an extraneous topic
            // was added (ie, we wrote topics in this code block but not
            // subscriptions, then no harm done.  The set removal in the
            // unsubscribe will simply remove nothing.
            Set<String> topics = this.getSet(subscriptionId);
            topics.add(topic);
            Set<String> subscribers = this.getSet(topic);
            subscribers.add(subscriptionId);
            return Observable.just(subscriptionId);
        }).subscribeOn(Schedulers.io());
    }

    private Observable<String> localUnsubscribe(final String subscriptionId) {
        // This is somewhat problematic but unavoidable in a distributed
        // system without ACID support.  We wait until after all topics
        // have had the subscription deleted before we delete the
        // subscription set.  In this way, we can repair.
        final Set<String> topics = this.getSet(subscriptionId);
        // it's important that this is idempotent
        for (String topic : topics) {
            final Set<String> subscriptions = this.getSet(topic);
            subscriptions.remove(subscriptionId);
        }

        this.deleteSubscription(subscriptionId);
        return Observable.just(subscriptionId);
    }

    @Override
    public Observable<String> unsubscribe(String subscriptionId) {
        return Observable.defer(() -> this.localUnsubscribe(subscriptionId).retry(5))
                .subscribeOn(Schedulers.io());
    }

    private Observable<String> localAcknowledge(final String subscriptionId, final Iterable<String> ackIds) {
        final Set<String> messageIds = Sets.newHashSet(ackIds);
        final BlockingQueue<MemoryMessage> messages = this.getQueue(subscriptionId);
        messages.removeIf(msg -> messageIds.contains(msg.getMessage().getId()));

        final BlockingQueue<MemoryMessage> deadMessages = this.getDeadLetterQueue(subscriptionId);
        deadMessages.removeIf(msg -> messageIds.contains(msg.getMessage().getId()));
        return Observable.just(subscriptionId);
    }

    @Override
    public Observable<String> acknowledge(final String subscriptionId, final Iterable<String> ackIds) {
        return Observable.defer(
                () -> this.localAcknowledge(subscriptionId, ackIds).retry(5)
        ).subscribeOn(Schedulers.io());
    }

    private Observable<List<String>> localPublish(final String topic,
                                                  final Iterable<ClientMessage> clientMessages) {

        final List<MemoryMessage> memoryMessages = StreamSupport
                .stream(clientMessages.spliterator(), false)
                .map(cm -> {
                    final String messageId = IdUtil.generateId();
                    return new MemoryMessage(new Message(
                            cm.getMessage(),
                            messageId,
                            topic,
                            messageId
                    ));
                })
                .collect(Collectors.toList());
        final Set<String> subscriptions = this.getSet(topic);
        try {
            for (String subscription : subscriptions) {
                BlockingQueue<MemoryMessage> bq = this.getQueue(subscription);
                // this does not guarantee ordering
                bq.addAll(memoryMessages);
            }
        } catch (Exception e) {
            return Observable.error(e);
        }

        return Observable.just(memoryMessages.stream()
                .map(rm -> rm.getMessage().getId())
                .collect(Collectors.toList())
        );
    }

    @Override
    public Observable<List<String>> publish(final String topic,
                                            final Iterable<ClientMessage> messages) {

        return Observable.defer(() -> this.localPublish(topic, messages).retry(5))
                .subscribeOn(Schedulers.io());
    }

    private MemoryMessage fetchMessage(final boolean returnImmediately,
                                       final String subscriptionId) throws InterruptedException {

        final BlockingQueue<MemoryMessage> queue = this.getQueue(subscriptionId);
        final BlockingQueue<MemoryMessage> deadLetterQueue = this.getDeadLetterQueue(subscriptionId);
        final MemoryMessage memoryMessage;

        if (returnImmediately) {
            memoryMessage = queue.poll();
        } else {
            memoryMessage = queue.poll(this.config.getClientLongPollingTimeout(), TimeUnit.SECONDS);
        }

        if (memoryMessage != null) {
            deadLetterQueue.add(memoryMessage);
        }

        return memoryMessage;
    }

    private Observable<List<Message>> localPull(final boolean returnImmediately,
                                                final String subscriptionId) throws InterruptedException {

        final MemoryMessage message = this.fetchMessage(returnImmediately, subscriptionId);
        if (message != null) {
            final Message clientMessage = new Message(
                    message.getMessage(),
                    message.getMessage().getAckId()
            );
            return Observable.just(Collections.singletonList(clientMessage));
        }

        return Observable.just(Collections.emptyList());
    }

    @Override
    public Observable<List<Message>> pull(final boolean returnImmediately,
                                          final String subscriptionId) {

        return Observable.defer(() -> this.localPull(returnImmediately, subscriptionId).retry(5))
                .subscribeOn(Schedulers.io());
    }

    public Observable<List<String>> getSubscriptions(final String topic) {
        return Observable.defer(() ->
                Observable.just(this.getSet(topic).stream().collect(Collectors.toList()))
        ).subscribeOn(Schedulers.io());
    }
}
