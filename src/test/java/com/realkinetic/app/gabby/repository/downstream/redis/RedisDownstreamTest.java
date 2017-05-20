package com.realkinetic.app.gabby.repository.downstream.redis;

import com.google.common.collect.Lists;
import com.realkinetic.app.gabby.base.BaseObservableTest;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Maybe;
import io.reactivex.observers.TestObserver;
import io.reactivex.schedulers.Schedulers;
import org.junit.*;

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RedisDownstreamTest extends BaseObservableTest {
    private static final Logger LOG = Logger.getLogger(RedisDownstreamTest.class.getName());
    private static RedisDownstream redisDownstream;

    @BeforeClass
    public static void beforeClass() {
        redisDownstream = new RedisDownstream(DefaultConfig.load());
    }

    @SuppressWarnings("unchecked")
    private <T> TestObserver getTestObserver() {
        return new TestObserver<T>();
    }

    @Test
    public void testSubscribe() {
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        redisDownstream.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertValue(subscriptionId);

        List<String> subscriptions = Lists.newArrayList(redisDownstream.getSubscriptions(topic));
        Assert.assertTrue(subscriptions.size() == 1);
    }

    @Test
    public void testUnsubscribe() {
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        redisDownstream.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);
        obs.assertValue(subscriptionId);

        TestObserver<List<String>> finalObs = this.getTestObserver();

        redisDownstream.unsubscribe(subscriptionId).subscribe(finalObs);
        this.advance();

        finalObs.awaitDone(10, TimeUnit.SECONDS);
        finalObs.assertNoErrors();
        finalObs.assertValue(ids -> ids.size() == 0);

        List<String> subscriptions = Lists.newArrayList(redisDownstream.getSubscriptions(topic));
        Assert.assertTrue(subscriptions.size() == 0);
    }

    @Test
    public void testPull() {
        String topic = IdUtil.generateId();
        String subscriptionId = IdUtil.generateId();
        TestObserver<String> obs = this.<String>getTestObserver();
        redisDownstream.subscribe(topic, subscriptionId).subscribe(obs);
        this.advance();
        obs.awaitDone(10, TimeUnit.SECONDS);

        TestObserver<Maybe<Message>> mobs = this.getTestObserver();
        redisDownstream.pull(subscriptionId)
                .subscribe(mobs);

        TestObserver<List<String>> sobs = this.getTestObserver();
        redisDownstream.publish(new Message("test", "ackid", topic, IdUtil.generateId()))
                .subscribe(sobs);

        this.advance();

        sobs.awaitDone(10, TimeUnit.SECONDS);
        sobs.assertNoErrors();
        mobs.awaitDone(10, TimeUnit.SECONDS);
        mobs.assertNoErrors();


        LOG.info("awaiting done");

        mobs.assertValue(msg -> msg.blockingGet().getMessage().equals("test"));
        sobs.assertValue(Lists.newArrayList(subscriptionId));
    }
}
