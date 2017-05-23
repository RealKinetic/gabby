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
import java.util.logging.Logger;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class MemoryDownstreamSubscription implements DownstreamSubscription {
    private static final Logger LOG = Logger.getLogger(MemoryDownstreamSubscription.class.getName());
    private final Config config;
    private final MemoryConfig memoryConfig;
    private final Map<String, Set<String>> sets;
    private final Map<String, BlockingQueue<MemoryMessage>> queues;
    private final Map<String, BlockingQueue<MemoryMessage>> deadLetterQueues;

    @Autowired
    public MemoryDownstreamSubscription(Config config) {
        this.config = config;
        this.memoryConfig = config.getMemoryConfig();
        this.sets = new ConcurrentSkipListMap<>();
        this.queues = new ConcurrentSkipListMap<>();
        this.deadLetterQueues = new ConcurrentSkipListMap<>();
    }

    private Set<String> getSet(String key) {
        return this.sets.computeIfAbsent(key, k -> new ConcurrentSkipListSet<>());
    }

    private BlockingQueue<MemoryMessage> getQueue(String key) {
        return this.queues.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());
    }

    private BlockingQueue<MemoryMessage> getDeadLetterQueue(String key) {
        return this.deadLetterQueues.computeIfAbsent(key, k -> new LinkedBlockingQueue<>());
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

            Observable.timer(this.config.getDownstreamTimeout(), TimeUnit.SECONDS)
                    .subscribe($ -> {
                        MemoryMessage last = deadLetterQueue.peek();
                        while (last != null && last.getTimestamp() < System.currentTimeMillis() - this.config.getDownstreamTimeout() * 1000) {
                            // if valid, we're going to move the deadletter to the main queue
                            // if not, mark the message as acknowledged
                            // ensure we add to the main queue before deleting from deadletter
                            if (last.getNumAccesses() < this.memoryConfig.getMaxAccesses() - 1) {
                                MemoryMessage copy = new MemoryMessage(last);
                                last.touch();
                                queue.add(last);
                                last = copy; // ensures remove works properly
                            } else {
                                this.acknowledge(
                                        subscriptionId,
                                        Collections.singleton(last.getMessage().getId())
                                ).subscribe();
                            }

                            deadLetterQueue.remove(last);
                            last = deadLetterQueue.peek();
                        }
                    });
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
