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
package com.realkinetic.app.gabby.controller;

import com.realkinetic.app.gabby.config.Config;
import com.realkinetic.app.gabby.model.dto.*;
import com.realkinetic.app.gabby.service.MessagingService;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.validation.Valid;
import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.logging.Logger;

@RestController
public class MessageController {
    private static final long CLIENT_TIMEOUT = 30 * 1000; // timeout time in milliseconds, ie, 30 seconds
    private static Logger LOG = Logger.getLogger(MessageController.class.getName());
    private final MessagingService messagingService;
    private final Config config;

    @Autowired
    public MessageController(MessagingService messagingService, Config config) {
        this.messagingService = messagingService;
        this.config = config;
    }

    @RequestMapping(value = "/subscriptions", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity<String>> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) throws IOException {
        DeferredResult<ResponseEntity<String>> dr = new DeferredResult<>(CLIENT_TIMEOUT);
        this.messagingService.subscribe(request.getTopic(), request.getSubscriptionId()).subscribe($ -> {
            dr.setResult(ResponseEntity.ok(request.getSubscriptionId()));
        });
        return dr;
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}", method = RequestMethod.DELETE)
    public DeferredResult<ResponseEntity.BodyBuilder> deleteSubscription(@PathVariable String subscriptionId) throws IOException {
        DeferredResult<ResponseEntity.BodyBuilder> dr = new DeferredResult<>(CLIENT_TIMEOUT);
        this.messagingService.unsubscribe(subscriptionId).subscribe($ -> {
            dr.setResult(ResponseEntity.ok());
        });
        return dr;
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}/messages", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity<Iterable<Message>>> pull(@PathVariable String subscriptionId) throws IOException {
        DeferredResult<ResponseEntity<Iterable<Message>>> dr
                = new DeferredResult<>((long) this.config.getClientLongPollingTimeout() * 1000);
        this.messagingService.pull(false, subscriptionId).subscribe(messages -> {
            dr.setResult(ResponseEntity.ok(messages));
        });
        return dr;
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}/ack", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity<Iterable<String>>> acknowledge(@PathVariable String subscriptionId,
                                                                        @RequestBody AcknowledgeMessagesRequest ack) throws IOException {

        DeferredResult<ResponseEntity<Iterable<String>>> dr = new DeferredResult<>(CLIENT_TIMEOUT);
        this.messagingService.acknowledge(subscriptionId, ack.getAckIds()).subscribe($ -> {
           dr.setResult(ResponseEntity.ok(ack.getAckIds()));
        });
        return dr;
    }

    @RequestMapping(value = "/topics/{topicId}/messages", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity<List<String>>> send(@PathVariable final String topicId,
                                                             @RequestBody final List<ClientMessage> messages) throws IOException {

        DeferredResult<ResponseEntity<List<String>>> dr = new DeferredResult<>(CLIENT_TIMEOUT);
        this.messagingService.publish(topicId, messages).subscribe(messageIds -> {
            dr.setResult(ResponseEntity.ok(messageIds));
        });
        return dr;
    }
}
