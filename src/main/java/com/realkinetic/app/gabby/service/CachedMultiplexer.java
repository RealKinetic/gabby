package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.repository.UpstreamSubscription;
import io.reactivex.Observable;
import io.reactivex.Observer;
import io.reactivex.disposables.Disposable;
import org.springframework.beans.factory.annotation.Autowired;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class CachedMultiplexer implements MultiplexingService, Observer<Message> {
    private final static Logger LOG = Logger.getLogger(CachedMultiplexer.class.getName());
    private final static long DELAY_TIME = 1;
    private final static TimeUnit DELAY_UNIT = TimeUnit.SECONDS;

    private final UpstreamSubscription upstreamSubscription;
    private final DownstreamSubscription downstreamSubscription;

    @Autowired
    public CachedMultiplexer(UpstreamSubscription upstreamSubscription,
                             DownstreamSubscription downstreamSubscription) {
        this.upstreamSubscription = upstreamSubscription;
        this.downstreamSubscription = downstreamSubscription;
        this.upstreamSubscription.listen().retryWhen(errors -> errors.flatMap(err -> {
            LOG.warning("encountered error from upstream subscription: " + err.getMessage());
            return Observable.timer(DELAY_TIME, DELAY_UNIT);
        })).subscribe(this);
    }

    @Override
    public String createSubscription(String topic) throws IOException {
        return null;
    }

    @Override
    public Observable<Message> listen(String subscriptionId) throws IOException {
        return null;
    }

    @Override
    public void acknowledge(Iterable<String> ackIds) throws IOException {

    }

    @Override
    public void onSubscribe(Disposable disposable) {

    }

    @Override
    public void onNext(Message message) {

    }

    @Override
    public void onError(Throwable throwable) {

    }

    @Override
    public void onComplete() {

    }
}