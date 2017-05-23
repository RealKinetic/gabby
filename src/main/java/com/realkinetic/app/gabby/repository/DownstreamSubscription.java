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
package com.realkinetic.app.gabby.repository;

import com.realkinetic.app.gabby.model.dto.ClientMessage;
import com.realkinetic.app.gabby.model.dto.Message;
import io.reactivex.Observable;

import java.util.List;

public interface DownstreamSubscription {
    // returns the subscription id
    Observable<String> subscribe(String topic, String subscriptionId);
    // returns subscription id
    Observable<String> unsubscribe(String subscriptionId);
    // returns the subscriptionid
    Observable<String> acknowledge(String subscriptionId, Iterable<String> ackIds);
    // returns the message id
    Observable<String> publish(ClientMessage message);
    // this will be null in the case of a timeout, force client to resubscribe
    Observable<List<Message>> pull(boolean returnImmediately, String subscriptionId);
    // returns a list of subscription ids for the provided topic
    Observable<List<String>> getSubscriptions(String topic);
}
