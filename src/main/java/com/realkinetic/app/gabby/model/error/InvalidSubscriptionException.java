package com.realkinetic.app.gabby.model.error;

public class InvalidSubscriptionException extends Exception {
    public InvalidSubscriptionException(String subscriptionId) {
        super(String.format("subscription %s cannot be found", subscriptionId));
    }
}
