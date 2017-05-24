package com.realkinetic.app.gabby.repository.downstream.memory;

import com.realkinetic.app.gabby.config.BaseConfig;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.config.MemoryConfig;
import com.realkinetic.app.gabby.repository.BaseDownstream;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import org.junit.Assert;
import org.junit.Test;

import java.util.logging.Logger;

public class MemoryDownstreamTest extends BaseDownstream {
    private static final Logger LOG = Logger.getLogger(MemoryDownstreamTest.class.getName());

    @Override
    protected DownstreamSubscription getDownstream() {
        final BaseConfig config = (BaseConfig) DefaultConfig.load();
        config.setDownstreamTimeout(1);
        final MemoryConfig memoryConfig = new MemoryConfig();
        memoryConfig.setMaxAccesses(10);
        config.setMemoryConfig(memoryConfig);
        return new MemoryDownstream(config, mm -> true);
    }

    @Override
    protected void createTopic(String topic) {
        // just like redis, this simply doesn't matter
    }

    @Test
    public void testFail() {
        Assert.fail();
    }
}
