package com.realkinetic.app.gabby.repository.downstream.redis;

import com.google.common.collect.Sets;
import com.realkinetic.app.gabby.config.Config;
import com.realkinetic.app.gabby.model.dto.ClientMessage;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;
import org.redisson.Redisson;
import org.redisson.api.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class RedisDownstream implements DownstreamSubscription {
    private static final Logger LOG = Logger.getLogger(RedisDownstream.class.getName());
    public static final int MAX_ACCESSES = 10;
    private final Config config;
    private final RedissonClient rc;

    @Autowired
    public RedisDownstream(Config config) {
        this.config = config;
        org.redisson.config.Config redissonConfig = new org.redisson.config.Config();
        redissonConfig.useSingleServer()
                .setAddress("127.0.0.1:6379")
                .setConnectionPoolSize(1000);

        this.rc = Redisson.create(redissonConfig);
    }

    private RSet<String> getSet(String key) {
        return this.rc.getSet(key);
    }

    private RBlockingDeque<ClientMessage> getQueue(String key) {
        return this.rc.getBlockingDeque(key + ":queue");
    }

    private RBlockingDeque<ClientMessage> getDeadLetterQueue(String key) {
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

            LOG.info("about to get set");
            Set<String> topics = this.getSet(subscriptionId);
            topics.add(topic);
            Set<String> subscribers = this.getSet(topic);
            subscribers.add(subscriptionId);
            return Observable.just(subscriptionId);
        }).subscribeOn(Schedulers.io());
    }

    private Observable<List<String>> localUnsubscribe(String subscriptionId) {
        // This is somewhat problematic but unavoidable in a distributed
        // system without ACID support.  We wait until after all topics
        // have had the subscription deleted before we delete the
        // subscription set.  In this way, we can repair.
        LOG.info("unsubscribing topic");
        RSet<String> topics = this.getSet(subscriptionId);
        LOG.info("topics retrieved");
        // it's important that this is idempotent
        for (String topic : topics) {
            RSet<String> subscriptions = this.getSet(topic);
            subscriptions.remove(subscriptionId);
        }

        LOG.info("topics unsubscribed");

        LinkedList<String> messageIds = new LinkedList<>();
        RBlockingDeque<ClientMessage> messages = this.getQueue(subscriptionId);
        LOG.info("message queue retrieved");
        messages.forEach(msg -> {
            messageIds.push(msg.getMessage().getId());
        });

        RBlockingDeque<ClientMessage> deadMessages = this.getDeadLetterQueue(subscriptionId);
        deadMessages.forEach(msg -> {
            messageIds.push(msg.getMessage().getId());
        });

        LOG.info("queues emptied");

        messages.delete();
        deadMessages.delete();
        topics.delete();

        return Observable.just(messageIds);
    }

    @Override
    public Observable<List<String>> unsubscribe(String subscriptionId) {
        return Observable.defer(() -> this.localUnsubscribe(subscriptionId).retry(5))
                .subscribeOn(Schedulers.io());
    }

    private Observable<String> localAcknowledge(final String subscriptionId, final Iterable<String> messageIds) {
        Set<String> ids = Sets.newHashSet(messageIds);
        RBlockingDeque<ClientMessage> messages = this.getQueue(subscriptionId);
        messages.removeIf(msg -> ids.contains(msg.getMessage().getId()));

        RBlockingDeque<ClientMessage> deadMessages = this.getDeadLetterQueue(subscriptionId);
        messages.removeIf(msg -> ids.contains(msg.getMessage().getId()));
        return Observable.just(subscriptionId);
    }

    @Override
    public Observable<String> acknowledge(final String subscriptionId, final Iterable<String> messageIds) {
        return Observable.defer(
                () -> this.localAcknowledge(subscriptionId, messageIds)
        ).subscribeOn(Schedulers.io());
    }

    private Observable<List<String>> localPublish(final Message message) {
        LinkedList<String> subscriptionIds = new LinkedList<>();
        Set<String> subscriptions = this.getSet(message.getTopic());
        try {
            for (String subscription : subscriptions) {
                this.getQueue(subscription).putFirst(new ClientMessage(message));
                subscriptionIds.push(subscription);
            }
        } catch (Exception e) {
            return Observable.error(e);
        }

        return Observable.just(subscriptionIds);
    }

    @Override
    public Observable<List<String>> publish(final Message message) {
        return Observable.defer(() -> this.localPublish(message).retry(5))
                .subscribeOn(Schedulers.io());
    }

    private Observable<List<Message>> localPull(final boolean returnImmediately,
                                                 final String subscriptionId) {

        ClientMessage message;
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
                        RBlockingDeque<ClientMessage> messages = this.getQueue(subscriptionId);
                        RBlockingDeque<ClientMessage> deadLetter = this.getDeadLetterQueue(subscriptionId);
                        ClientMessage last = deadLetter.peekLast();
                        while (last != null && last.getTimestamp() < System.currentTimeMillis() - this.config.getDownstreamTimeout() * 1000) {
                            // if valid, we're going to move the deadletter to the main queue
                            // if not, mark the message as acknowledged
                            // ensure we add to the main queue before deleting from deadletter
                            if (last.getNumAccesses() < MAX_ACCESSES - 1) {
                                ClientMessage copy = new ClientMessage(last);
                                last.touch();
                                messages.putFirst(last);
                                last = copy; // ensures remove works properly
                            } else {
                                this.acknowledge(subscriptionId, Collections.singleton(last.getMessage().getId()));
                            }

                            deadLetter.remove(last);
                            last = deadLetter.peekLast();
                        }
                    });

            return Observable.just(Collections.singletonList(message.getMessage()));
        }

        return Observable.just(Collections.emptyList());
    }

    @Override
    public Observable<List<Message>> pull(final boolean returnImmediately,
                                           final String subscriptionId) {

        return Observable.defer(() -> this.localPull(returnImmediately, subscriptionId).retry(5))
                .subscribeOn(Schedulers.io());
    }

    public Iterable<String> getSubscriptions(final String topic) {
        return this.getSet(topic).stream().collect(Collectors.toList());
    }
}
