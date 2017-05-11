package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.MessageResponse;
import io.reactivex.Observable;

import java.io.IOException;

public class CachedMultiplexer implements MultiplexingService {
    @Override
    public String createSubscription(String topic) throws IOException {
        return null;
    }

    @Override
    public Observable<MessageResponse> listen(String subscriptionId) throws IOException {
        return null;
    }

    @Override
    public void acknowledge(Iterable<String> ackIds) throws IOException {

    }
}
