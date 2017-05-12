package com.realkinetic.app.gabby.repository;

import com.realkinetic.app.gabby.model.MessageResponse;
import io.reactivex.Observable;

import java.io.IOException;

public interface UpstreamSubscription {
    Observable<MessageResponse> listen();
    void acknowledge(Iterable<String> ackIds) throws IOException;
    void push(String topic, String message) throws IOException;
}
