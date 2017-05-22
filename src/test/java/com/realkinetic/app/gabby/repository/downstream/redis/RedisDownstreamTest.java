package com.realkinetic.app.gabby.repository.downstream.redis;

import com.realkinetic.app.gabby.config.BaseConfig;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.config.RedisConfig;
import com.realkinetic.app.gabby.repository.BaseDownstream;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import org.junit.*;

import java.util.Collections;
import java.util.logging.Logger;

public class RedisDownstreamTest extends BaseDownstream {
    private static final Logger LOG = Logger.getLogger(RedisDownstreamTest.class.getName());
    private static RedisDownstream redisDownstream;

    @BeforeClass
    public static void beforeClass() {
        BaseConfig config = (BaseConfig) DefaultConfig.load();
        config.setDownstreamTimeout(0);
        final RedisConfig redisConfig = new RedisConfig();
        redisConfig.setConnectionPoolSize(100);
        redisConfig.setHosts(Collections.singletonList("127.0.0.1:6379"));
        redisConfig.setMaxAccesses(10);
        config.setRedisConfig(redisConfig);
        redisDownstream = new RedisDownstream(config);
    }

    @Override
    protected DownstreamSubscription getDownstream() {
        return redisDownstream;
    }

    @Override
    protected void createTopic(String topic) {
        // don't need to do anything with redis
    }
}
