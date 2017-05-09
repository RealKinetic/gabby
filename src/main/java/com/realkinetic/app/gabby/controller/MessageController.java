package com.realkinetic.app.gabby.controller;

import com.realkinetic.app.gabby.model.MessageResponse;
import com.realkinetic.app.gabby.model.dto.AcknowledgeMessagesRequest;
import com.realkinetic.app.gabby.model.dto.CreateSubscriptionRequest;
import com.realkinetic.app.gabby.service.MessagingService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.IOException;

@RestController
public class MessageController {

    @Autowired
    MessagingService messagingService;

    @RequestMapping(value = "/subscriptions", method = RequestMethod.POST)
    public ResponseEntity<String> createSubscription(@RequestBody CreateSubscriptionRequest request) throws IOException {
        return ResponseEntity.ok(this.messagingService.createSubscription(request.getName()));
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}", method = RequestMethod.DELETE)
    public void deleteSubscription(@PathVariable String subscriptionId) throws IOException {
        this.messagingService.deleteSubscription(subscriptionId);
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}", method = RequestMethod.GET)
    public ResponseEntity<Iterable<MessageResponse>> pull(@PathVariable String subscriptionId) throws IOException {
        return ResponseEntity.ok(this.messagingService.pull(subscriptionId));
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}/ack", method = RequestMethod.POST)
    public void acknowledge(@PathVariable String subscriptionId, @RequestBody AcknowledgeMessagesRequest ack) throws IOException {
        this.messagingService.acknowledge(subscriptionId, ack.getAckIds());
    }
}
