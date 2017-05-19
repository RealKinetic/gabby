package com.realkinetic.app.gabby.base;

import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import org.junit.After;
import org.junit.Before;

public class BaseObservableTest {
    protected TestScheduler testScheduler;

    @Before
    public void before() {
        this.testScheduler = new TestScheduler();
        RxJavaPlugins.setComputationSchedulerHandler($ -> this.testScheduler);
    }

    @After
    public void after() {
        RxJavaPlugins.setComputationSchedulerHandler(null); // reset
    }
}
