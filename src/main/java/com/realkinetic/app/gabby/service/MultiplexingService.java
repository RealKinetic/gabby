package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.MessageResponse;
import io.reactivex.Observable;

import java.io.IOException;

public interface MultiplexingService {
    String createSubscription(String topic) throws IOException;
    Observable<MessageResponse> listen(String subscriptionId) throws IOException;
    void acknowledge(Iterable<String> ackIds) throws IOException;
}
