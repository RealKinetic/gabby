package com.realkinetic.app.gabby.repository.downstream.memory;

import com.realkinetic.app.gabby.base.BaseObservableTest;
import org.junit.Before;

public class TestMessageBroker extends BaseObservableTest {
    private MessageBroker messageBroker;

    @Before
    public void before() {
        super.before();
        this.messageBroker = new MessageBroker("topic", "subscriptionId");
    }


}
