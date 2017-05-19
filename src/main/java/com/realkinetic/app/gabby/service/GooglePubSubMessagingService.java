package com.realkinetic.app.gabby.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.*;
import com.google.common.collect.ImmutableList;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.model.dto.CreateSubscriptionRequest;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.common.collect.Lists.newArrayList;

public class GooglePubSubMessagingService implements MessagingService {
    private static final Logger log = Logger.getLogger(GooglePubSubMessagingService.class.getName());
    private static final String APP_NAME = "gabby";
    private static final String PROJECT = "rk-playground";
    private final Pubsub pubsub;

    public GooglePubSubMessagingService() throws IOException {
        this(
                // default credentials take environment auth or GCE/appengine when deployed
                new RetryHttpInitializerWrapper(GoogleCredential.getApplicationDefault()),
                Utils.getDefaultTransport(),
                Utils.getDefaultJsonFactory()
        );
    }

    public GooglePubSubMessagingService(final HttpRequestInitializer initializer,
                                        final HttpTransport transport,
                                        final JsonFactory factory) {

        this.pubsub = new Pubsub.Builder(transport, factory, initializer)
                .setApplicationName(APP_NAME)
                .build();
    }

    @Override
    public String createSubscription(CreateSubscriptionRequest request) throws IOException {
        String topic = getFullyQualifiedResourceName(ResourceType.TOPIC, PROJECT, request.getName());
        Subscription subscription = new Subscription().setTopic(topic);
        String name = generateId();
        String qualified = getFullyQualifiedResourceName(ResourceType.SUBSCRIPTION, PROJECT, name);
        this.pubsub.projects()
                .subscriptions()
                .create(qualified, subscription)
                .execute();

        return name;
    }

    @Override
    public void deleteSubscription(String subscriptionName) throws IOException {
        this.pubsub.projects()
                .subscriptions()
                .delete(getFullyQualifiedResourceName(ResourceType.SUBSCRIPTION, PROJECT, subscriptionName))
                .execute();
    }

    @Override
    public Observable<Iterable<Message>> pull(String subscriptionName) throws IOException {
        return Observable.defer(() -> {
            try {
                return Observable.just(longPull(subscriptionName));
            } catch (IOException e) {
                return Observable.error(e);
            }
        }).subscribeOn(Schedulers.newThread());  // or we could use a named thread
    }

    private Iterable<Message> longPull(String subscriptionName) throws IOException {
        subscriptionName = getFullyQualifiedResourceName(
                ResourceType.SUBSCRIPTION,
                PROJECT,
                subscriptionName
        );
        PullRequest pullRequest = new PullRequest()
                .setReturnImmediately(false)
                .setMaxMessages(20);

        PullResponse pullResponse = this.pubsub.projects()
                .subscriptions()
                .pull(subscriptionName, pullRequest)
                .execute();

        List<Message> responses = new ArrayList<>();
        List<ReceivedMessage> receivedMessages =
                pullResponse.getReceivedMessages();

        if (receivedMessages != null) {
            for (ReceivedMessage receivedMessage : receivedMessages) {
                PubsubMessage pubsubMessage =
                        receivedMessage.getMessage();

                String body = null;
                if (pubsubMessage != null
                        && pubsubMessage.decodeData() != null) {
                    body = new String(pubsubMessage.decodeData(),
                            "UTF-8");
                }
                responses.add(
                        new Message(body, receivedMessage.getAckId(), "", IdUtil.generateId())
                );
            }
        }

        return responses;
    }

    @Override
    public void acknowledge(String subscriptionName, Iterable<String> ackIds) throws IOException {
        AcknowledgeRequest acknowledgeRequest = new AcknowledgeRequest()
                .setAckIds(newArrayList(ackIds));

        subscriptionName = getFullyQualifiedResourceName(
                ResourceType.SUBSCRIPTION,
                PROJECT,
                subscriptionName
        );
        this.pubsub.projects()
                .subscriptions()
                .acknowledge(subscriptionName, acknowledgeRequest)
                .execute();
    }

    @Override
    public void send(String topic, String message) throws IOException {
        topic = getFullyQualifiedResourceName(ResourceType.TOPIC, PROJECT, topic);
        PubsubMessage pubsubMessage = new PubsubMessage()
                .encodeData(message.getBytes("UTF-8"));
        List<PubsubMessage> messages = ImmutableList.of(pubsubMessage);
        PublishRequest publishRequest = new PublishRequest();
        publishRequest.setMessages(messages);
        PublishResponse publishResponse = this.pubsub.projects()
                .topics()
                .publish(topic, publishRequest)
                .execute();
    }

    // the following was pulled from google's sample cli app
    /**
     * Enum representing a resource type.
     */
    public enum ResourceType {
        /**
         * Represents topics.
         */
        TOPIC("topics"),
        /**
         * Represents subscriptions.
         */
        SUBSCRIPTION("subscriptions");
        /**
         * A path representation for the resource.
         */
        private String collectionName;
        /**
         * A constructor.
         *
         * @param collectionName String representation of the resource.
         */
        private ResourceType(final String collectionName) {
            this.collectionName = collectionName;
        }
        /**
         * Returns its collection name.
         *
         * @return the collection name.
         */
        public String getCollectionName() {
            return this.collectionName;
        }
    }

    public static String getFullyQualifiedResourceName(
            final ResourceType resourceType, final String project,
            final String resource) {
        return String.format("projects/%s/%s/%s", project,
                resourceType.getCollectionName(), resource);
    }

    private static String generateId() {
        String name = UUID.randomUUID().toString().replaceAll("[\\s\\-()]", "");
        // uuids can start with a number, sub names must start with a letter
        Random r = new Random();
        char c = (char) (r.nextInt(6) + 'a');
        return c + name.substring(1);
    }
}
