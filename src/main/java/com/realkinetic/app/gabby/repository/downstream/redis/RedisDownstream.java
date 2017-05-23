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
package com.realkinetic.app.gabby.repository.downstream.redis;

import com.google.common.collect.Sets;
import com.realkinetic.app.gabby.config.Config;
import com.realkinetic.app.gabby.config.RedisConfig;
import com.realkinetic.app.gabby.model.dto.ClientMessage;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RedisDownstream implements DownstreamSubscription {
    private static final Logger LOG = Logger.getLogger(RedisDownstream.class.getName());
    private final Config config;
    private final RedissonClient rc;
    private final RedisConfig redisConfig;

    @Autowired
    public RedisDownstream(Config config) {
        this.config = config;
        this.redisConfig = config.getRedisConfig();
        org.redisson.config.Config redissonConfig = new org.redisson.config.Config();
        redissonConfig.useSingleServer()
                .setAddress(this.redisConfig.getHosts().get(0))
                .setConnectionPoolSize(this.redisConfig.getConnectionPoolSize());

        this.rc = Redisson.create(redissonConfig);
    }

    private RSet<String> getSet(String key) {
        return this.rc.getSet(key);
    }

    private RBlockingDeque<RedisMessage> getQueue(String key) {
        return this.rc.getBlockingDeque(key + ":queue");
    }

    private RBlockingDeque<RedisMessage> getDeadLetterQueue(String key) {
        return this.getQueue(key + ":deadletter");
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

    private Observable<String> localUnsubscribe(String subscriptionId) {
        // This is somewhat problematic but unavoidable in a distributed
        // system without ACID support.  We wait until after all topics
        // have had the subscription deleted before we delete the
        // subscription set.  In this way, we can repair.
        final RSet<String> topics = this.getSet(subscriptionId);
        // it's important that this is idempotent
        for (String topic : topics) {
            final RSet<String> subscriptions = this.getSet(topic);
            subscriptions.remove(subscriptionId);
        }

        final RBlockingDeque<RedisMessage> messages = this.getQueue(subscriptionId);
        final RBlockingDeque<RedisMessage> deadMessages = this.getDeadLetterQueue(subscriptionId);

        messages.delete();
        deadMessages.delete();
        topics.delete();

        return Observable.just(subscriptionId);
    }

    @Override
    public Observable<String> unsubscribe(String subscriptionId) {
        return Observable.defer(() -> this.localUnsubscribe(subscriptionId).retry(5))
                .subscribeOn(Schedulers.io());
    }

    private Observable<String> localAcknowledge(final String subscriptionId, final Iterable<String> ackIds) {
        final Set<String> messageIds = Sets.newHashSet(ackIds);
        final RBlockingDeque<RedisMessage> messages = this.getQueue(subscriptionId);
        messages.removeIf(msg -> messageIds.contains(msg.getMessage().getId()));

        final RBlockingDeque<RedisMessage> deadMessages = this.getDeadLetterQueue(subscriptionId);
        deadMessages.removeIf(msg -> messageIds.contains(msg.getMessage().getId()));
        return Observable.just(subscriptionId);
    }

    @Override
    public Observable<String> acknowledge(final String subscriptionId, final Iterable<String> ackIds) {
        return Observable.defer(
                () -> this.localAcknowledge(subscriptionId, ackIds).retry(5)
        ).subscribeOn(Schedulers.io());
    }

    private Observable<String> localPublish(final ClientMessage clientMessage) {
        final String messageId = IdUtil.generateId();
        final Message message = new Message(
                clientMessage.getMessage(),
                messageId,
                clientMessage.getTopic(),
                messageId
        );
        final Set<String> subscriptions = this.getSet(message.getTopic());
        try {
            for (String subscription : subscriptions) {
                this.getQueue(subscription).putFirst(new RedisMessage(message));
            }
        } catch (Exception e) {
            return Observable.error(e);
        }

        return Observable.just(message.getId());
    }

    @Override
    public Observable<String> publish(final ClientMessage message) {
        return Observable.defer(() -> this.localPublish(message).retry(5))
                .subscribeOn(Schedulers.io());
    }

    private Observable<List<Message>> localPull(final boolean returnImmediately,
                                                final String subscriptionId) {

        final RedisMessage message;
        try {
            if (returnImmediately) {
                message = this.getQueue(subscriptionId)
                        .pollLastAndOfferFirstTo(this.getDeadLetterQueue(subscriptionId).getName());
            } else {
                message = this.getQueue(subscriptionId).pollLastAndOfferFirstTo(
                        this.getDeadLetterQueue(subscriptionId).getName(),
                        this.config.getDownstreamTimeout(),  // doesn't have to be this setting
                        TimeUnit.SECONDS
                );
            }

        } catch (Exception e) {
            return Observable.error(e);
        }

        if (message != null) {
            Observable.timer(this.config.getDownstreamTimeout(), TimeUnit.SECONDS)
                    .subscribe($ -> {
                        RBlockingDeque<RedisMessage> messages = this.getQueue(subscriptionId);
                        RBlockingDeque<RedisMessage> deadLetter = this.getDeadLetterQueue(subscriptionId);
                        RedisMessage last = deadLetter.peekLast();
                        while (last != null && last.getTimestamp() < System.currentTimeMillis() - this.config.getDownstreamTimeout() * 1000) {
                            // if valid, we're going to move the deadletter to the main queue
                            // if not, mark the message as acknowledged
                            // ensure we add to the main queue before deleting from deadletter
                            if (last.getNumAccesses() < this.redisConfig.getMaxAccesses() - 1) {
                                RedisMessage copy = new RedisMessage(last);
                                last.touch();
                                messages.putFirst(last);
                                last = copy; // ensures remove works properly
                            } else {
                                this.acknowledge(
                                        subscriptionId,
                                        Collections.singleton(last.getMessage().getId())
                                ).subscribe();
                            }

                            deadLetter.remove(last);
                            last = deadLetter.peekLast();
                        }
                    });

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
        );
    }
}
