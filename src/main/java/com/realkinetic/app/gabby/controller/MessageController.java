package com.realkinetic.app.gabby.controller;

import com.realkinetic.app.gabby.model.MessageResponse;
import com.realkinetic.app.gabby.model.dto.AcknowledgeMessagesRequest;
import com.realkinetic.app.gabby.model.dto.CreateMessageRequest;
import com.realkinetic.app.gabby.model.dto.CreateSubscriptionRequest;
import com.realkinetic.app.gabby.service.MessagingService;
import io.reactivex.Observable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.request.async.DeferredResult;

import javax.validation.Valid;
import java.io.IOException;
import java.util.logging.Logger;

@RestController
public class MessageController {
    private static Logger log = Logger.getLogger(MessageController.class.getName());

    @Autowired
    MessagingService messagingService;

    @RequestMapping(value = "/subscriptions", method = RequestMethod.POST)
    public ResponseEntity<String> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) throws IOException {
        return ResponseEntity.ok(this.messagingService.createSubscription(request));
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}", method = RequestMethod.DELETE)
    public void deleteSubscription(@PathVariable String subscriptionId) throws IOException {
        this.messagingService.deleteSubscription(subscriptionId);
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}/messages", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity<Iterable<MessageResponse>>> pull(@PathVariable String subscriptionId) throws IOException {
        log.info("starting pull");
        DeferredResult<ResponseEntity<Iterable<MessageResponse>>> dr = new DeferredResult<>();
        Observable<Iterable<MessageResponse>> obs = this.messagingService.pull(subscriptionId);
        obs.subscribe(responses -> dr.setResult(ResponseEntity.ok(responses)));
        log.info("returning poll"); // prove this is thread is being returned to the spring pool
        return dr;
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}/ack", method = RequestMethod.POST)
    public void acknowledge(@PathVariable String subscriptionId, @RequestBody AcknowledgeMessagesRequest ack) throws IOException {
        this.messagingService.acknowledge(subscriptionId, ack.getAckIds());
    }

    @RequestMapping(value = "/topics/{topicId}/messages", method = RequestMethod.POST)
    public void send(@PathVariable String topicId, @RequestBody CreateMessageRequest msg) throws IOException {
        this.messagingService.send(topicId, msg.getMessage());
    }
}
