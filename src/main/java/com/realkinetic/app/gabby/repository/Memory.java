package com.realkinetic.app.gabby.repository;

import com.google.common.collect.ImmutableSet;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentSkipListSet;

public class Memory implements Subscriber {
    private Map<String, Set<String>> subscribers;

    public Memory() {
        this.subscribers = new ConcurrentHashMap<String, Set<String>>(10);
    }

    @Override
    public String register(String topic) throws IOException {
        String id = generateId();
        this.subscribers.compute(topic, (k, set) -> {
            if (set == null) {
                set = new ConcurrentSkipListSet<>();
            }
            set.add(id);
            return set;
        });
        return id;
    }

    @Override
    public void deregister(String topic, String subscriptionId) throws IOException {
        this.subscribers.computeIfPresent(topic, (k, set) -> {
            set.remove(subscriptionId);
            if (set.size() == 0) {
                return null;
            }

            return set;
        });
    }

    public ImmutableSet<String> getSubscribers(String topic) {
        Set<String> value = this.subscribers.get(topic);
        if (value == null) {
            value = new HashSet<>();
        }
        return ImmutableSet.<String>builder().addAll(value).build();
    }

    private static String generateId() {
        String name = UUID.randomUUID().toString().replaceAll("[\\s\\-()]", "");
        // uuids can start with a number, sub names must start with a letter
        Random r = new Random();
        char c = (char) (r.nextInt(6) + 'a');
        return c + name.substring(1);
    }
}
