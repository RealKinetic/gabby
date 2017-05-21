package com.realkinetic.app.gabby.repository;

import com.google.common.collect.ImmutableSet;
import com.realkinetic.app.gabby.util.MultithreadedUtil;
import org.junit.Before;
import org.junit.Test;
import org.springframework.util.Assert;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class TestMemory {
    private Memory memory;

    @Before
    public void before() {
        this.memory = new Memory();
    }

    @Test
    public void testRegister() throws IOException {
        String id = this.memory.register("topic");
        Assert.notNull(id);
        Assert.isTrue(id.length() > 0);
    }

    @Test
    public void testGetSubscribers() throws IOException {
        String id = this.memory.register("topic");
        ImmutableSet<String> results = this.memory.getSubscribers("topic");
        Assert.isTrue(results.size() == 1);
        Assert.isTrue(results.contains(id));
    }

    @Test
    public void testGetSubscribersMultipleSubscriptions() throws IOException {
        String id1 = this.memory.register("topic1");
        String id2 = this.memory.register("topic2");
        ImmutableSet<String> results1 = this.memory.getSubscribers("topic1");
        ImmutableSet<String> results2 = this.memory.getSubscribers("topic2");
        Assert.isTrue(results1.size() == 1);
        Assert.isTrue(results2.size() == 1);
        Assert.isTrue(results1.contains(id1));
        Assert.isTrue(results2.contains(id2));
    }

    @Test
    public void testUnsubscribe() throws IOException {
        String id = this.memory.register("topic");
        this.memory.deregister("topic", id);
        ImmutableSet<String> results = this.memory.getSubscribers("topic");
        Assert.isTrue(results.size() == 0);
    }

    @Test
    public void testUnsubscribeEmptyMemory() throws IOException {
        this.memory.deregister("topic", "id");
        ImmutableSet<String> results = this.memory.getSubscribers("topic");
        Assert.isTrue(results.size() == 0);
    }

    @Test
    public void testMultithreadedPut() throws IOException, InterruptedException {
        int numThreads = 1000;
        List<String> ids = Collections.synchronizedList(new ArrayList<>());
        List<Runnable> runnables = new ArrayList<>();

        for (int i = 0; i < numThreads; i++) {
            runnables.add(() -> {
                try {
                    String id = this.memory.register("topic");
                    ids.add(id);
                } catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        MultithreadedUtil.assertConcurrent(
                "fail on multithreaded subscribe", runnables, 1000
        );

        Assert.isTrue(ids.size() == numThreads);
        ImmutableSet<String> results = this.memory.getSubscribers("topic");
        Assert.isTrue(results.size() == numThreads);
        for (String id: ids) {
            Assert.isTrue(results.contains(id));
        }
    }

    @Test
    public void testMultithreadedPutAndRead() throws IOException, InterruptedException {
        int numThreads = 1000;
        List<String> ids = Collections.synchronizedList(new ArrayList<>(numThreads));
        List<Runnable> runnables = new ArrayList<>(numThreads);

        for (int i = 0; i < numThreads; i++) {
            int j = i;
            runnables.add(() -> {
                try {
                    // half the time we write, half read
                    if (j % 2 == 0) {
                        String id = this.memory.register("topic");
                        ids.add(id);
                    } else {
                        this.memory.getSubscribers("topic");
                    }
                } catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        MultithreadedUtil.assertConcurrent(
                "fail on multithreaded subscribe", runnables, 1000
        );

        Assert.isTrue(ids.size() == numThreads/2);
        ImmutableSet<String> results = this.memory.getSubscribers("topic");
        Assert.isTrue(results.size() == numThreads/2);
        for (String id: ids) {
            Assert.isTrue(results.contains(id));
        }
    }

    @Test
    public void testMultithreadedPutAndDelete() throws IOException, InterruptedException {
        int numThreads = 1000;
        List<Runnable> runnables = new ArrayList<>(numThreads);

        for (int i = 0; i < numThreads; i++) {
            runnables.add(() -> {
                try {
                    String id = this.memory.register("topic");
                    this.memory.deregister("topic", id);
                } catch(IOException e) {
                    throw new UncheckedIOException(e);
                }
            });
        }

        MultithreadedUtil.assertConcurrent(
                "fail on multithreaded subscribe", runnables, 1000
        );

        ImmutableSet<String> results = this.memory.getSubscribers("topic");
        Assert.isTrue(results.size() == 0);
    }
}