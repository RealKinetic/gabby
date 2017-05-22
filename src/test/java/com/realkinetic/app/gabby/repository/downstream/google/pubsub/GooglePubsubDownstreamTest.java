package com.realkinetic.app.gabby.repository.downstream.google.pubsub;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.realkinetic.app.gabby.config.BaseConfig;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.config.GooglePubsubConfig;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.BaseDownstream;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.observers.TestObserver;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class GooglePubsubDownstreamTest extends BaseDownstream {
    private static final Logger LOG = Logger.getLogger(GooglePubsubDownstreamTest.class.getName());
    private static GooglePubsubDownstream pubsubDownstream;

    @BeforeClass
    public static void beforeClass() throws IOException {
        final BaseConfig config = (BaseConfig) DefaultConfig.load();
        config.setDownstreamTimeout(0);
        final GooglePubsubConfig pubsubConfig = new GooglePubsubConfig();
        pubsubConfig.setAppName("gabby");
        pubsubConfig.setProject("static-files-rk");
        config.setGooglePubsubConfig(pubsubConfig);
        pubsubDownstream = new GooglePubsubDownstream(config);
    }

    @Override
    protected DownstreamSubscription getDownstream() {
        return pubsubDownstream;
    }

    @Override
    public void testPullDeadLetterMaxAccesses() {
        // this feature isn't supported in google pub sub
    }

    @Override
    protected void createTopic(String topic) {
        TestObserver<String> obs = this.getTestObserver();
        pubsubDownstream.createTopic(topic).subscribe(obs);

        this.advance();

        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertNoErrors();
    }

    @Override
    @Test
    public void testPull() {
        // we don't know what the result of a publish will be, just generated
        // ids from google's side
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        this.createTopic(topic);
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
        sobs.assertValue(msgs -> msgs.size() == 1);
    }

    @Override
    @Test
    public void testMultiplePull() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        this.createTopic(topic);
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
    }

    @Override
    @Test
    public void testPullDeadLetter() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        this.createTopic(topic);
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
        try {
            Thread.sleep(15L*1000L);
        } catch (Exception e) {
            Assert.fail(e.getMessage());
        }
        ds.pull(false, subscriptionId)
                .subscribe(mobs);

        this.advance();

        mobs.awaitDone(10, TimeUnit.SECONDS);
        mobs.assertNoErrors();
        mobs.assertValue(msg -> msg.get(0).getMessage().equals("test"));
    }

    @Test
    public void testAcknowledge() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        this.createTopic(topic);
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
        mobs1.assertValueCount(1);

        TestObserver<String> aobs = this.getTestObserver();
        ds.acknowledge(subscriptionId, Collections.singleton(mobs1.values().get(0).get(0).getAckId()))
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
    @Override
    public void testUnsubscribeCleansQueues() {
        // not supported in pubsub
    }
}