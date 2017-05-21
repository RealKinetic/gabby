package com.realkinetic.app.gabby.controller;

import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.model.dto.AcknowledgeMessagesRequest;
import com.realkinetic.app.gabby.model.dto.CreateMessageRequest;
import com.realkinetic.app.gabby.model.dto.CreateSubscriptionRequest;
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
import java.util.logging.Logger;

@RestController
public class MessageController {
    private static Logger log = Logger.getLogger(MessageController.class.getName());
    private final MessagingService messagingService;

    @Autowired
    public MessageController(MessagingService messagingService) {
        this.messagingService = messagingService;
    }

    @RequestMapping(value = "/subscriptions", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity<String>> createSubscription(@Valid @RequestBody CreateSubscriptionRequest request) throws IOException {
        DeferredResult<ResponseEntity<String>> dr = new DeferredResult<>(30L);
        this.messagingService.subscribe(request.getTopic(), request.getSubscriptionId()).subscribe($ -> {
            dr.setResult(ResponseEntity.ok(request.getSubscriptionId()));
        });
        return dr;
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}", method = RequestMethod.DELETE)
    public DeferredResult<ResponseEntity.BodyBuilder> deleteSubscription(@PathVariable String subscriptionId) throws IOException {
        DeferredResult<ResponseEntity.BodyBuilder> dr = new DeferredResult<>(30L);
        this.messagingService.unsubscribe(subscriptionId).subscribe($ -> {
            dr.setResult(ResponseEntity.ok());
        });
        return dr;
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}/messages", method = RequestMethod.GET)
    public DeferredResult<ResponseEntity<Iterable<Message>>> pull(@PathVariable String subscriptionId) throws IOException {
        log.info("starting pull");
        DeferredResult<ResponseEntity<Iterable<Message>>> dr = new DeferredResult<>();
        this.messagingService.pull(false, subscriptionId).subscribe(messages -> {
            dr.setResult(ResponseEntity.ok(messages));
        });
        log.info("returning poll"); // prove this is thread is being returned to the spring pool
        return dr;
    }

    @RequestMapping(value = "/subscriptions/{subscriptionId}/ack", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity<Iterable<String>>> acknowledge(@PathVariable String subscriptionId, @RequestBody AcknowledgeMessagesRequest ack) throws IOException {
        DeferredResult<ResponseEntity<Iterable<String>>> dr = new DeferredResult<>(30L);
        this.messagingService.acknowledge(subscriptionId, ack.getAckIds()).subscribe($ -> {
           dr.setResult(ResponseEntity.ok(ack.getAckIds()));
        });
        return dr;
    }

    @RequestMapping(value = "/topics/{topicId}/messages", method = RequestMethod.POST)
    public DeferredResult<ResponseEntity<String>> send(@PathVariable final String topicId, @RequestBody final CreateMessageRequest msg) throws IOException {
        String messageId = IdUtil.generateId();
        Message message = new Message(msg.getMessage(), IdUtil.generateId(), topicId, messageId);
        DeferredResult<ResponseEntity<String>> dr = new DeferredResult<>(30L);
        this.messagingService.publish(message).subscribe($ -> {
            dr.setResult(ResponseEntity.ok(messageId));
        });
        return dr;
    }
}
