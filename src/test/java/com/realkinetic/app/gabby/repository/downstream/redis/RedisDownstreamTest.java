package com.realkinetic.app.gabby.repository.downstream.redis;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.realkinetic.app.gabby.base.BaseObservableTest;
import com.realkinetic.app.gabby.config.BaseConfig;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.model.dto.Message;
import com.realkinetic.app.gabby.repository.BaseDownstream;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.util.IdUtil;
import io.reactivex.observers.TestObserver;
import org.junit.*;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

public class RedisDownstreamTest extends BaseDownstream {
    private static final Logger LOG = Logger.getLogger(RedisDownstreamTest.class.getName());
    private static RedisDownstream redisDownstream;

    @BeforeClass
    public static void beforeClass() {
        BaseConfig config = (BaseConfig) DefaultConfig.load();
        config.setDownstreamTimeout(0);
        redisDownstream = new RedisDownstream(config);
    }

    @Override
    protected DownstreamSubscription getDownstream() {
        return redisDownstream;
    }
}
