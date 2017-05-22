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

import com.realkinetic.app.gabby.model.dto.Message;
import io.reactivex.Observable;

public interface UpstreamSubscription {
    Observable<Message> listen();
    Observable<Void> acknowledge(Iterable<String> ackIds);
    Observable<Void> push(String topic, String message);
}
