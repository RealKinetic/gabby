package com.realkinetic.app.gabby.base;

import io.reactivex.plugins.RxJavaPlugins;
import io.reactivex.schedulers.TestScheduler;
import org.junit.After;
import org.junit.Before;

import java.util.concurrent.TimeUnit;

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

    public void advance() {
        this.advanceBy(10);
    }

    public void advanceBy(final long amount) {
        this.advanceBy(amount, TimeUnit.SECONDS);
    }

    public void advanceBy(final long amount, final TimeUnit unit) {
        this.testScheduler.advanceTimeBy(amount, unit);
    }
}
