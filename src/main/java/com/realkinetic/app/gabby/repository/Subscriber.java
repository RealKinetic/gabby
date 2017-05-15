package com.realkinetic.app.gabby.repository;

import io.reactivex.Observable;

public interface Subscriber {
    Observable<String> register(String topic);
    Observable<Void> deregister(String topic, String subscriptionId);
}
