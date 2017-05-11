package com.realkinetic.app.gabby.repository;

import java.io.IOException;

public interface Subscriber {
    String register(String topic) throws IOException;
    void deregister(String topic, String subscriptionId) throws IOException;
}
