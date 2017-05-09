package com.realkinetic.app.gabby.service;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.*;
import com.realkinetic.app.gabby.model.MessageResponse;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.UUID;
import java.util.logging.Logger;

import static com.google.common.collect.Lists.newArrayList;

@Service
public class GooglePubSubMessagingService implements MessagingService {
    private static final Logger log = Logger.getLogger(GooglePubSubMessagingService.class.getName());
    private static final String APP_NAME = "gabby";
    private static final String PROJECT = "rk-playground";
    private final Pubsub pubsub;

    public GooglePubSubMessagingService() throws IOException {
        this(
                GoogleCredential.getApplicationDefault(),
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
    public String createSubscription(String topic) throws IOException {
        topic = getFullyQualifiedResourceName(ResourceType.TOPIC, PROJECT, topic);
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
    public Iterable<MessageResponse> pull(String subscriptionName) throws IOException {
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

        List<MessageResponse> responses = new ArrayList<>();
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
                        new MessageResponse(body, receivedMessage.getAckId())
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

    /**
     * Returns the fully qualified resource name for Pub/Sub.
     *
     * @param resourceType ResourceType.
     * @param project A project id.
     * @param resource topic name or subscription name.
     * @return A string in a form of PROJECT_NAME/RESOURCE_NAME
     */
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
