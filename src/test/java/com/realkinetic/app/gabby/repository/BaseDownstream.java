package com.realkinetic.app.gabby.repository;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.realkinetic.app.gabby.base.BaseObservableTest;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.observers.TestObserver;
import org.junit.Assert;
import org.junit.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;

public abstract class BaseDownstream extends BaseObservableTest {
    @SuppressWarnings("unchecked")
    private <T> TestObserver getTestObserver() {
        return new TestObserver<T>();
    }

    protected abstract DownstreamSubscription getDownstream();

    @Test
    public void testSubscribe() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertValue(subscriptionId);

        TestObserver<List<String>> subscribers = this.getTestObserver();
        ds.getSubscriptions(topic).subscribe(subscribers);
        this.advance();

        subscribers.awaitDone(10, TimeUnit.SECONDS);
        subscribers.assertNoErrors();
        subscribers.assertValue(Collections.singletonList(subscriptionId));
    }

    @Test
    public void testUnsubscribe() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertValue(subscriptionId);

        TestObserver<List<String>> finalObs = this.getTestObserver();

        ds.unsubscribe(subscriptionId).subscribe(finalObs);
        this.advance();

        finalObs.awaitDone(10, TimeUnit.SECONDS);
        finalObs.assertNoErrors();
        finalObs.assertValue(ids -> ids.size() == 0);

        TestObserver<List<String>> subscribers = this.getTestObserver();
        ds.getSubscriptions(topic).subscribe(subscribers);
        this.advance();

        subscribers.awaitDone(10, TimeUnit.SECONDS);
        subscribers.assertNoErrors();
        subscribers.assertValue(Collections.emptyList());
    }

    @Test
    public void testPull() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);

        TestObserver<List<Message>> mobs = this.getTestObserver();
        ds.pull(false, subscriptionId)
                .subscribe(mobs);

        TestObserver<List<String>> sobs = this.getTestObserver();
        ds.publish(new Message("test", "ackid", topic, IdUtil.generateId()))
                .subscribe(sobs);

        this.advance();

        sobs.awaitDone(10, TimeUnit.SECONDS);
        sobs.assertNoErrors();
        mobs.awaitDone(10, TimeUnit.SECONDS);
        mobs.assertNoErrors();

        mobs.assertValue(msg -> msg.get(0).getMessage().equals("test"));
        sobs.assertValue(Lists.newArrayList(subscriptionId));
    }

    @Test
    public void testMultiplePull() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId1 = IdUtil.generateId();
        String subscriptionId2 = IdUtil.generateId();
        TestObserver<String> obs1 = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId1).subscribe(obs1);
        this.advance();
        obs1.awaitDone(10, TimeUnit.SECONDS);

        TestObserver<String> obs2 = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId2).subscribe(obs2);
        this.advance();
        obs2.awaitDone(10, TimeUnit.SECONDS);

        TestObserver<List<Message>> mobs1 = this.getTestObserver();
        ds.pull(false, subscriptionId1)
                .subscribe(mobs1);

        TestObserver<List<Message>> mobs2 = this.getTestObserver();
        ds.pull(false, subscriptionId2)
                .subscribe(mobs2);

        TestObserver<List<String>> sobs = this.getTestObserver();
        ds.publish(new Message("test", "ackid", topic, IdUtil.generateId()))
                .subscribe(sobs);

        this.advance();

        sobs.awaitDone(10, TimeUnit.SECONDS);
        sobs.assertNoErrors();
        mobs1.awaitDone(10, TimeUnit.SECONDS);
        mobs1.assertNoErrors();
        mobs2.awaitDone(10, TimeUnit.SECONDS);
        mobs2.assertNoErrors();

        mobs1.assertValue(msg -> msg.get(0).getMessage().equals("test"));
        mobs2.assertValue(msg -> msg.get(0).getMessage().equals("test"));
        sobs.assertValue(ids -> {
            Set<String> expected = Sets.newHashSet(subscriptionId1, subscriptionId2);
            Set<String> actual = Sets.newHashSet(ids);
            return expected.equals(actual);
        });
    }

    @Test
    public void testPullDeadLetter() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);

        TestObserver<List<String>> sobs = this.getTestObserver();
        ds.publish(new Message("test", "ackid", topic, IdUtil.generateId()))
                .subscribe(sobs);

        TestObserver<List<Message>> mobs = this.getTestObserver();
        ds.pull(false, subscriptionId)
                .subscribe(mobs);

        this.advance();

        sobs.awaitDone(10, TimeUnit.SECONDS);
        sobs.assertNoErrors();
        mobs.awaitDone(10, TimeUnit.SECONDS);
        mobs.assertNoErrors();

        // we should also get a message here as the item made its way back
        // on the main queue
        mobs = this.getTestObserver();
        ds.pull(false, subscriptionId)
                .subscribe(mobs);

        this.advance();

        mobs.awaitDone(10, TimeUnit.SECONDS);
        mobs.assertNoErrors();

        mobs.assertValue(msg -> msg.get(0).getMessage().equals("test"));
        sobs.assertValue(Lists.newArrayList(subscriptionId));
    }

    @Test
    public void testPullDeadLetterMaxAccesses() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);

        TestObserver<List<String>> sobs = this.getTestObserver();
        ds.publish(new Message("test", "ackid", topic, IdUtil.generateId()))
                .subscribe(sobs);

        this.advance();

        sobs.awaitDone(10, TimeUnit.SECONDS);
        sobs.assertNoErrors();

        for (int i = 0; i <= 10; i++) { // need to change this
            TestObserver<List<Message>> mobs = this.getTestObserver();
            ds.pull(true, subscriptionId).subscribe(mobs);

            this.advance();
            mobs.awaitDone(10, TimeUnit.SECONDS);
            mobs.assertNoErrors();
            this.advance();
            if (i < 10) {
                mobs.assertValue(msg -> msg.get(0).getMessage().equals("test"));
            } else {
                mobs.assertValue(Collections.emptyList());
            }
        }
    }

    @Test
    public void testAcknowledge() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);
        Message msg = new Message("test", "ackid", topic, IdUtil.generateId());

        TestObserver<List<String>> sobs = this.getTestObserver();
        ds.publish(msg)
                .subscribe(sobs);

        TestObserver<List<Message>> mobs1 = this.getTestObserver();
        ds.pull(false, subscriptionId).subscribe(mobs1);

        this.advance();

        sobs.awaitDone(10, TimeUnit.SECONDS);
        sobs.assertNoErrors();
        mobs1.awaitDone(10, TimeUnit.SECONDS);
        mobs1.assertNoErrors();

        TestObserver<String> aobs = this.getTestObserver();
        ds.acknowledge(subscriptionId, Collections.singleton(msg.getId()))
                .subscribe(aobs);

        this.advance();
        aobs.awaitDone(10, TimeUnit.SECONDS);
        aobs.assertNoErrors();
        this.advance();

        mobs1 = this.getTestObserver();
        ds.pull(true, subscriptionId).subscribe(mobs1);

        this.advance();
        mobs1.awaitDone(10, TimeUnit.SECONDS);
        mobs1.assertNoErrors();
        mobs1.assertValue(Collections.emptyList());
    }

    @Test
    public void testUnsubscribeCleansQueues() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertValue(subscriptionId);
        Message msg = new Message("test", "ackid", topic, IdUtil.generateId());

        TestObserver<List<String>> sobs = this.getTestObserver();
        ds.publish(msg)
                .subscribe(sobs);

        this.advance();

        sobs.awaitDone(10, TimeUnit.SECONDS);
        sobs.assertNoErrors();

        TestObserver<List<String>> finalObs = this.getTestObserver();

        ds.unsubscribe(subscriptionId).subscribe(finalObs);
        this.advance();

        finalObs.awaitDone(10, TimeUnit.SECONDS);
        finalObs.assertNoErrors();
        finalObs.assertValue(Collections.singletonList(msg.getId()));

        obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertValue(subscriptionId);

        TestObserver<List<Message>> mobs = this.getTestObserver();
        ds.pull(true, subscriptionId).subscribe(mobs);
        this.advance();

        mobs.awaitDone(10, TimeUnit.SECONDS);
        mobs.assertNoErrors();
        mobs.assertValue(Collections.emptyList());
    }
}
