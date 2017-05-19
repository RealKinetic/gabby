package com.realkinetic.app.gabby.repository;

import com.realkinetic.app.gabby.model.dto.Message;
import io.reactivex.Observable;

public interface UpstreamSubscription {
    Observable<Message> listen();
    Observable<Void> acknowledge(Iterable<String> ackIds);
    Observable<Void> push(String topic, String message);
}
