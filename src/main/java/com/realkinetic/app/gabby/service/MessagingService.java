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
import io.reactivex.Observable;

import java.util.List;

public interface MessagingService {
    // returns the subscription id
    Observable<String> subscribe(String topic, String subscriptionId);
    // returns a list of message ids that this subscription has not yet
    // acknowledged.
    Observable<List<String>> unsubscribe(String subscriptionId);
    // returns the subscriptionid
    Observable<String> acknowledge(String subscriptionId, Iterable<String> ackIds);
    // returns a list of subscriptionIds that were "notified" of the publish
    Observable<List<String>> publish(Message message);
    // this will be null in the case of a timeout, force client to resubscribe
    Observable<List<Message>> pull(boolean returnImmediately, String subscriptionId);
}
