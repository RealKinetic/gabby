/*
Copyright 2017 Real Kinetic LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
*/
package com.realkinetic.app.gabby.service;

import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import com.realkinetic.app.gabby.util.LambdaExceptionUtil;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

public class StandAloneMessagingService implements MessagingService {
    private final DownstreamSubscription downstreamSubscription;

    @Autowired
    public StandAloneMessagingService(DownstreamSubscription downstreamSubscription) {
        this.downstreamSubscription = downstreamSubscription;
    }

    @Override
    public Observable<String> subscribe(String topic, String subscriptionId) {
        return this.downstreamSubscription.subscribe(topic, subscriptionId);
    }

    @Override
    public Observable<List<String>> unsubscribe(String subscriptionId) {
        return this.downstreamSubscription.unsubscribe(subscriptionId);
    }

    @Override
    public Observable<String> acknowledge(String subscriptionId, Iterable<String> ackIds) {
        return Observable.defer(() -> {
           List<String> messageIds = StreamSupport
                   .stream(ackIds.spliterator(), false)
                   .map(LambdaExceptionUtil.rethrowFunction(IdUtil::getMessageIdFromAckId))
                   .collect(Collectors.toList());
           return this.downstreamSubscription.acknowledge(subscriptionId, messageIds);
        });
    }

    @Override
    public Observable<List<String>> publish(Message message) {
        return this.downstreamSubscription.publish(message);
    }

    @Override
    public Observable<List<Message>> pull(boolean returnImmediately, String subscriptionId) {
        return this.downstreamSubscription.pull(returnImmediately, subscriptionId).map(messages -> {
            return messages.stream()
                    .map(message -> new Message(message, IdUtil.generateAckId(subscriptionId, message.getId())))
                    .collect(Collectors.toList());
        });
    }
}
