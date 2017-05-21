package com.realkinetic.app.gabby.repository;

import com.realkinetic.app.gabby.model.MessageResponse;
import io.reactivex.observers.TestObserver;
import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

public class TestMemoryUpstreamSubscription {
    private static Logger log = Logger.getLogger(TestMemoryUpstreamSubscription.class.getName());
    private static List<String> topics = Arrays.asList("topic1", "topic2");
    private MemoryUpstreamSubscription upstream;
    private TestScheduler testScheduler;

    @Before
    public void before() {
        this.upstream = new MemoryUpstreamSubscription(topics);
        this.testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler($ -> this.testScheduler);
    }

    @After
    public void after() {
        RxJavaPlugins.setComputationSchedulerHandler(null); // reset
    }

    @Test
    public void testListen() throws IOException, InterruptedException {
        TestObserver<MessageResponse> obs = new TestObserver<>();
        this.upstream.listen().subscribe(obs);
        this.upstream.push("topic1", "message");
        this.testScheduler.advanceTimeBy(
                MemoryUpstreamSubscription.retryTime/2,
                MemoryUpstreamSubscription.timeUnit
        );
        obs.awaitCount(1);
        obs.assertValueCount(1);
        obs.assertNotTerminated();
        obs.assertValue(mr ->
           mr.getMessage().equals("message")
                && mr.getTopic().equals("topic1")
        );
    }

    @Test
    public void testListenRetries() throws IOException, InterruptedException {
        TestObserver<MessageResponse> obs = new TestObserver<>();
        this.upstream.listen().subscribe(obs);
        this.upstream.push("topic1", "message");
        this.testScheduler.advanceTimeBy(
                MemoryUpstreamSubscription.retryTime,
                MemoryUpstreamSubscription.timeUnit
        );
        obs.awaitCount(2);
        obs.assertValueCount(2);
        obs.assertNotTerminated();

        obs.cancel();
        this.testScheduler.advanceTimeBy(
                MemoryUpstreamSubscription.retryTime,
                MemoryUpstreamSubscription.timeUnit
        );
        obs.assertValueCount(2);
    }

    @Test
    public void testAcknowledge() throws IOException, InterruptedException {
        TestObserver<MessageResponse> obs = new TestObserver<>();
        this.upstream.listen().subscribe(obs);
        this.upstream.push("topic1", "message");
        this.testScheduler.advanceTimeBy(
                MemoryUpstreamSubscription.retryTime,
                MemoryUpstreamSubscription.timeUnit
        );
        obs.awaitCount(2);
        obs.assertValueCount(2);
        obs.assertNotTerminated();

        List<List<Object>> objects = obs.getEvents();
        for (List<Object> i : objects) {
            for (Object j : i) {
                MessageResponse mr = (MessageResponse) j;
                this.upstream.acknowledge(Collections.singletonList(mr.getAckId()));
            }
        }

        this.testScheduler.advanceTimeBy(
                MemoryUpstreamSubscription.retryTime,
                MemoryUpstreamSubscription.timeUnit
        );

        obs.assertValueCount(2);
    }

    @Test(expected = IOException.class)
    public void testListenOnNonExistentTopic() throws IOException {
        this.upstream.push("random", "rando");
    }
}
