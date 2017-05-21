package com.realkinetic.app.gabby.model.dto;

import org.hibernate.validator.constraints.NotEmpty;

public class CreateSubscriptionRequest {
    public String getTopic() {
        return topic;
    }

    public void setTopic(String topic) {
        this.topic = topic;
    }

    public String getSubscriptionId() {
        return subscriptionId;
    }

    public void setSubscriptionId(String subscriptionId) {
        this.subscriptionId = subscriptionId;
    }

    @NotEmpty
    private String subscriptionId;

    @NotEmpty
    private String topic;
}
