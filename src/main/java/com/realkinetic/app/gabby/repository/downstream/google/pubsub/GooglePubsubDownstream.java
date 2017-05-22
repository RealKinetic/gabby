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
package com.realkinetic.app.gabby.repository.downstream.google.pubsub;

import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.util.Utils;
import com.google.api.client.http.HttpRequestInitializer;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.services.pubsub.Pubsub;
import com.google.api.services.pubsub.model.*;
import com.google.common.collect.ImmutableList;
import com.realkinetic.app.gabby.config.Config;
import com.realkinetic.app.gabby.config.GooglePubsubConfig;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.Observable;
import io.reactivex.schedulers.Schedulers;

import java.io.IOException;
import java.util.*;
import java.util.logging.Logger;
import java.util.stream.Collectors;

import static com.google.common.collect.Lists.newArrayList;

public class GooglePubsubDownstream implements DownstreamSubscription {
    private static final Logger LOG = Logger.getLogger(GooglePubsubDownstream.class.getName());
    private final Pubsub pubsub;
    private final GooglePubsubConfig pubsubConfig;
    private final Config config;

    public GooglePubsubDownstream(final Config config) throws IOException {
        this(
                config,
                // default credentials take environment auth or GCE/appengine when deployed
                new RetryHttpInitializerWrapper(
                        GoogleCredential.getApplicationDefault(),
                        config.getGooglePubsubConfig().getReadTimeout()
                ),
                Utils.getDefaultTransport(),
                Utils.getDefaultJsonFactory()
        );
    }

    public GooglePubsubDownstream(final Config config,
                                  final HttpRequestInitializer initializer,
                                  final HttpTransport transport,
                                  final JsonFactory factory) {
        this.config = config;
        this.pubsubConfig = config.getGooglePubsubConfig();
        this.pubsub = new Pubsub.Builder(transport, factory, initializer)
                .setApplicationName(this.pubsubConfig.getAppName())
                .build();
    }

    @Override
    public Observable<String> subscribe(final String topic, final String subscriptionId) {
        return Observable.defer(() -> {
            final String qualifiedTopic = getFullyQualifiedResourceName(ResourceType.TOPIC, this.pubsubConfig.getProject(), topic);
            final Subscription subscription = new Subscription()
                    .setAckDeadlineSeconds(this.config.getDownstreamTimeout())
                    .setTopic(qualifiedTopic);
            final String qualifiedSubscriptionId = getFullyQualifiedResourceName(ResourceType.SUBSCRIPTION, this.pubsubConfig.getProject(), subscriptionId);
            this.pubsub.projects()
                    .subscriptions()
                    .create(qualifiedSubscriptionId, subscription)
                    .execute();

            return Observable.just(subscriptionId);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<List<String>> unsubscribe(final String subscriptionId) {
        return Observable.defer(() -> {
            this.pubsub.projects()
                    .subscriptions()
                    .delete(getFullyQualifiedResourceName(ResourceType.SUBSCRIPTION, this.pubsubConfig.getProject(), subscriptionId))
                    .execute();

            return Observable.<List<String>>just(Collections.emptyList());
        }).subscribeOn(Schedulers.io());
    }

    public Observable<String> acknowledge(final String subscriptionId, final Iterable<String> ackIds) {
        return Observable.defer(() -> {
            final AcknowledgeRequest acknowledgeRequest = new AcknowledgeRequest()
                    .setAckIds(newArrayList(ackIds));

            final String qSubscriptionId = getFullyQualifiedResourceName(
                    ResourceType.SUBSCRIPTION,
                    this.pubsubConfig.getProject(),
                    subscriptionId
            );
            this.pubsub.projects()
                    .subscriptions()
                    .acknowledge(qSubscriptionId, acknowledgeRequest)
                    .execute();
            return Observable.just(subscriptionId);
        }).subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<List<String>> publish(final Message message) {
        return Observable.defer(() -> {
            final String qTopic = getFullyQualifiedResourceName(
                    ResourceType.TOPIC,
                    this.pubsubConfig.getProject(),
                    message.getTopic()
            );
            final PubsubMessage pubsubMessage = new PubsubMessage()
                    .encodeData(message.getMessage().getBytes("UTF-8"));
            final List<PubsubMessage> messages = ImmutableList.of(pubsubMessage);
            final PublishRequest publishRequest = new PublishRequest();
            publishRequest.setMessages(messages);
            PublishResponse publishResponse = this.pubsub.projects()
                    .topics()
                    .publish(qTopic, publishRequest)
                    .execute();
            return Observable.just(publishResponse.getMessageIds());
        }).subscribeOn(Schedulers.io());
    }

    private Observable<List<Message>> longPull(final boolean returnImmediately,
                                               final String subscriptionId) {
        return Observable.defer(() -> {
            final String qSubscriptionId = getFullyQualifiedResourceName(
                    ResourceType.SUBSCRIPTION,
                    this.pubsubConfig.getProject(),
                    subscriptionId
            );
            final PullRequest pullRequest = new PullRequest()
                    .setReturnImmediately(returnImmediately)
                    .setMaxMessages(20);

            final PullResponse pullResponse = this.pubsub.projects()
                    .subscriptions()
                    .pull(qSubscriptionId, pullRequest)
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

            return Observable.just(responses);
        });
    }

    @Override
    public Observable<List<Message>> pull(boolean returnImmediately, String subscriptionId) {
        return this.longPull(returnImmediately, subscriptionId)
                .subscribeOn(Schedulers.io());
    }

    @Override
    public Observable<List<String>> getSubscriptions(final String topic) {
        return Observable.defer(() -> {
            final List<String> subscriptionIds = new ArrayList<>();
            final String qualifiedTopic = getFullyQualifiedResourceName(
                    ResourceType.TOPIC, this.pubsubConfig.getProject(), topic
            );
            final Pubsub.Projects.Subscriptions.List lister
                    = this.pubsub.projects()
                    .subscriptions()
                    .list("projects/" + this.pubsubConfig.getProject());
            String nextPageToken = null;

            do {
                if (nextPageToken != null) {
                    lister.setPageToken(nextPageToken);
                }

                final ListSubscriptionsResponse response = lister.execute();
                if (!response.isEmpty()) {
                    subscriptionIds.addAll(response
                            .getSubscriptions()
                            .stream()
                            .filter(sub -> sub.getTopic().equals(qualifiedTopic))
                            .map(sub -> getResourceFromFullyQualifiedName(sub.getName()))
                            .collect(Collectors.toList()));
                }

                nextPageToken = response.getNextPageToken();
            } while (nextPageToken != null);



            return Observable.just(subscriptionIds);
        }).subscribeOn(Schedulers.io());
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

    private static String getResourceFromFullyQualifiedName(final String qualifiedName) {
        return qualifiedName.split("/")[3];
    }
}
