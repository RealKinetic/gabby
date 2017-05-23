package com.realkinetic.app.gabby.repository.downstream.google.pubsub;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.realkinetic.app.gabby.config.BaseConfig;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.config.GooglePubsubConfig;
import com.realkinetic.app.gabby.model.dto.ClientMessage;
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
    protected void createTopic(final String topic) {
        TestObserver<String> obs = this.getTestObserver();
        pubsubDownstream.createTopic(topic).subscribe(obs);

        this.advance();

        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertNoErrors();
    }

    @Test
    @Override
    public void testPullDeadLetterMaxAccesses() {
        // this feature isn't supported in google pub sub
    }

    @Test
    @Override
    public void testUnsubscribeCleansQueues() {
        // not supported in pubsub
    }

    @Test
    @Override
    public void testPullDeadLetter() {
        DownstreamSubscription ds = this.getDownstream();
        String topic = IdUtil.generateId();
        this.createTopic(topic);
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        ds.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);

        TestObserver<String> sobs = this.getTestObserver();
        ds.publish(new ClientMessage(topic, "test"))
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
}