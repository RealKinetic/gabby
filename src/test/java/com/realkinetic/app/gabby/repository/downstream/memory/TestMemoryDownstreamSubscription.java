package com.realkinetic.app.gabby.repository.downstream.memory;

import com.realkinetic.app.gabby.base.BaseObservableTest;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.observers.TestObserver;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class TestMemoryDownstreamSubscription extends BaseObservableTest {
    private static final Logger LOG = Logger.getLogger(TestMemoryDownstreamSubscription.class.getName());
    private DownstreamSubscription memorySubscriber;

    @Before
    public void before() {
        super.before();
        this.memorySubscriber = new MemoryDownstreamSubscription();
    }

    public String generateSubscriptionId() {
        return IdUtil.generateId();
    }

    @Test
    public void testRegister() {
        TestObserver<String> obs = new TestObserver<>();
        String subscriptionId = this.generateSubscriptionId();
        this.memorySubscriber.subscribe("topic", subscriptionId).subscribe(obs);
        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertValueCount(1);
        obs.assertValue(v -> {
            Assert.notNull(v);
            Assert.isTrue(v.length() > 0);
            return true;
        });
    }

    /*
    @Test
    public void testGetSubscribers() {
        TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
        AtomicReference<String> id = new AtomicReference<>();
        this.memorySubscriber.register("topic")
                .concatMap(generated -> {
                    id.set(generated);
                    return this.memorySubscriber.getSubscribers("topic");
                }).subscribe(obs);

        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertValueCount(1);

        ImmutableSet<String> expected = ImmutableSet.<String>builder().add(id.get()).build();
        obs.assertValue(expected);
    }

    @Test
    public void testGetSubscribersMultipleSubscriptions() {
        Function<String, Observable<ImmutableSet<String>>> f = (topic) -> {
            return this.memorySubscriber.register(topic)
                    .concatMap($ -> this.memorySubscriber.getSubscribers(topic));
        };
        TestObserver<ImmutableSet<String>> obs1 = new TestObserver<>();
        TestObserver<ImmutableSet<String>> obs2 = new TestObserver<>();

        f.apply("topic1").subscribe(obs1);
        f.apply("topic2").subscribe(obs2);

        obs1.awaitDone(10, TimeUnit.MILLISECONDS);
        obs2.awaitDone(10, TimeUnit.MILLISECONDS);

        obs1.assertValue(set -> set.size() == 1);
        obs2.assertValue(set -> set.size() == 1);
    }

    @Test
    public void testUnsubscribe() {
        TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
        this.memorySubscriber.register("topic")
                .concatMap(id -> {
                    // wonderful little hack to work around Void
                    return this.memorySubscriber.deregister("topic", id).map($ -> 1).defaultIfEmpty(5);
                })
                .concatMap($ -> this.memorySubscriber.getSubscribers("topic"))
                .subscribe(obs);

        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertNoErrors();
        obs.assertValue(set -> set.size() == 0);
    }

    @Test
    public void getEmptySubscribers() {
        TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
        this.memorySubscriber.getSubscribers("topic").subscribe(obs);

        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertValue(set -> set.size() == 0);
    }

    @Test
    public void testUnsubscribeEmptyMemory() throws IOException {
        TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
        this.memorySubscriber.deregister("topic", "id").map($ -> 1).defaultIfEmpty(5)
                .concatMap($ -> this.memorySubscriber.getSubscribers("topic"))
                .subscribe(obs);

        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertValue(set -> set.size() == 0);
    }

    @Test
    public void testMultithreadedPut() throws IOException, InterruptedException {
        int numThreads = 1000;
        List<TestObserver<String>> observers = Collections.synchronizedList(new ArrayList<>(numThreads));
        List<Runnable> runnables = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            runnables.add(() -> {
                TestObserver<String> obs = new TestObserver<>();
                this.memorySubscriber.register("topic").subscribe(obs);
                observers.add(obs);
            });
        }

        MultithreadedUtil.assertConcurrent(
                "fail on multithreaded subscribe", runnables, 1000
        );

        Assert.isTrue(observers.size() == numThreads);
        for (TestObserver<String> obs : observers) {
            obs.awaitDone(10, TimeUnit.MILLISECONDS);
        }

        TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
        this.memorySubscriber.getSubscribers("topic").subscribe(obs);
        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertValue(set -> set.size() == numThreads);
    }

    @Test
    public void testMultithreadedPutAndRead() throws IOException, InterruptedException {
        int numThreads = 1000;
        List<TestObserver<?>> observers = Collections.synchronizedList(new ArrayList<>(numThreads));
        List<Runnable> runnables = new ArrayList<>(numThreads);

        for (int i = 0; i < numThreads; i++) {
            int j = i;
            runnables.add(() -> {
                // half the time we write, half read
                if (j % 2 == 0) {
                    TestObserver<String> obs = new TestObserver<>();
                    this.memorySubscriber.register("topic").subscribe(obs);
                    observers.add(obs);
                } else {
                    TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
                    this.memorySubscriber.getSubscribers("topic").subscribe(obs);
                    observers.add(obs);
                }
            });
        }

        MultithreadedUtil.assertConcurrent(
                "fail on multithreaded subscribe", runnables, 1000
        );

        Assert.isTrue(observers.size() == numThreads);
        for (TestObserver<?> obs : observers) {
            obs.awaitDone(10, TimeUnit.MILLISECONDS);
        }

        TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
        this.memorySubscriber.getSubscribers("topic").subscribe(obs);

        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertValue(set -> set.size() == numThreads/2);
    }

    @Test
    public void testMultithreadedPutAndDelete() throws IOException, InterruptedException {
        int numThreads = 1000;
        List<Runnable> runnables = new ArrayList<>(numThreads);
        List<TestObserver<?>> observers = Collections.synchronizedList(new ArrayList<>(numThreads));

        for (int i = 0; i < numThreads; i++) {
            runnables.add(() -> {
                TestObserver<Void> obs = new TestObserver<>();
                this.memorySubscriber.register("topic")
                    .concatMap(s -> this.memorySubscriber.deregister("topic", s))
                    .subscribe(obs);
                observers.add(obs);
            });
        }

        MultithreadedUtil.assertConcurrent(
                "fail on multithreaded subscribe", runnables, 1000
        );

        Assert.isTrue(observers.size() == numThreads);

        for (TestObserver<?> obs : observers) {
            obs.awaitDone(10, TimeUnit.MILLISECONDS);
        }

        TestObserver<ImmutableSet<String>> obs = new TestObserver<>();
        this.memorySubscriber.getSubscribers("topic").subscribe(obs);

        obs.awaitDone(10, TimeUnit.MILLISECONDS);
        obs.assertValue(set -> set.size() == 0);
    }*/
}
