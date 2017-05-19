package com.realkinetic.app.gabby.repository.downstream.memory;

import com.realkinetic.app.gabby.model.dto.Message;
import io.reactivex.Maybe;
import io.reactivex.Observable;
import io.reactivex.disposables.Disposable;
import io.reactivex.schedulers.Schedulers;
import io.reactivex.subjects.PublishSubject;
import io.reactivex.subjects.Subject;

import java.util.Collections;
import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class MessageBroker {
    private static int MAX_CAPACITY = 100;
    private static int TIMEOUT = 10 * 60;  // 10 minutes

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public String getTopic() {
        return this.topic;
    }

    private final String topic;
    private final String subscriptionId;
    private final BlockingQueue<PrivateMessage> messages;
    private final Map<String, PrivateMessage> deadLetterQueue;
    private final Subject<Maybe<Message>> disposedObs;
    private final AtomicBoolean isDisposed;
    private final Map<String, Disposable> disposables;

    public MessageBroker(final String topic, final String subscriptionId) {
        this.subscriptionId = subscriptionId;
        this.messages = new LinkedBlockingQueue<>(MAX_CAPACITY);
        this.deadLetterQueue = new ConcurrentSkipListMap<>();
        this.disposedObs = PublishSubject.create();
        this.isDisposed = new AtomicBoolean(false);
        this.disposables = new ConcurrentSkipListMap<>();
        this.topic = topic;
    }

    public void push(final Message message) {
        // if this is disposed or full we don't really care and we'll pretend
        // to add
        PrivateMessage pm = new PrivateMessage(message);
        this.messages.offer(pm);
    }

    public Observable<Maybe<Message>> pull() {
        // there is a slight issue here in that it's possible for a user to
        // cancel a subscription while still holding onto this poll.
        return Observable.defer(() -> {
            if (this.isDisposed.get()) {
                return Observable.just(Maybe.<Message>empty());
            }
            PrivateMessage msg = this.messages.poll(TIMEOUT, TimeUnit.SECONDS);
            if (msg == null) { // did not hit the timeout
                return Observable.just(Maybe.<Message>empty());
            }

            if (msg.touch()) {
                this.deadLetterQueue.put(msg.getMessage().getId(), msg);
                Disposable disposable = Observable.timer(TIMEOUT, TimeUnit.SECONDS)
                        .take(1)
                        .subscribe($ -> {
                            this.deadLetterQueue.computeIfPresent(msg.getMessage().getId(), (k, pm) -> {
                                // the atomicity of this is under question, although it probably
                                // doesn't matter.  If we absolutely had to, we could
                                // use an exclusive lock here, but no data should be lost,
                                // should only cause a repeat.
                                if (this.isDisposed.get()) {
                                    return null;
                                }
                                this.messages.offer(pm);
                                return null;
                            });
                        });

                // there is still a race condition here between pulling items
                // and disposing.  That will eventually work itself out
                // due to the timer.  That's a rare corner case though and most
                // of the time we'll be able to kill all timers on dispose.
                this.disposables.put(msg.getMessage().getId(), disposable);
            }

            return Observable.just(Maybe.just(msg.getMessage()));
        })
                .mergeWith(this.disposedObs)
                .take(1)
                .subscribeOn(Schedulers.computation());
    }

    public void acknowledge(String messageId) {
        // first thing we're going to do is cancel the process that is listening
        // in the dead letter queue so that it does not try to add message
        // immediately after we delete it.
        this.disposables.remove(messageId);
        // then it is safe to go ahead and remove the item from the dead
        // letter queue
        this.deadLetterQueue.remove(messageId);
    }

    public Iterable<String> dispose() {
        final boolean old = this.isDisposed.getAndSet(true);
        if (old) { // someone has already disposed
            return Collections.emptyList();
        }

        this.disposedObs.onNext(Maybe.empty());
        this.disposedObs.onComplete();
        this.disposables.forEach(($, disposable) -> disposable.dispose());
        this.disposables.clear();
        LinkedList<String> unacknowledgedMessageIds = new LinkedList<>();
        // there's actually a slight race condition here using the concurrent
        // blocking queue/deadletter queue, but this should self-heal later.
        this.messages.stream()
                .map(pm -> pm.getMessage().getId())
                .forEach(unacknowledgedMessageIds::push);
        this.deadLetterQueue.forEach((s, $) -> unacknowledgedMessageIds.push(s));
        return unacknowledgedMessageIds;
    }
}