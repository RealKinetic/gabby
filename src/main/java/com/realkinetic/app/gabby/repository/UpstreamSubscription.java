package com.realkinetic.app.gabby.repository;

import com.realkinetic.app.gabby.model.MessageResponse;
import io.reactivex.Observable;

import java.io.IOException;

public interface UpstreamSubscription {
    Observable<MessageResponse> listen();
    Observable<Void> acknowledge(Iterable<String> ackIds);
    Observable<Void> push(String topic, String message);
}
