package com.realkinetic.app.gabby.repository.downstream.google.pubsub;

import com.realkinetic.app.gabby.config.BaseConfig;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.config.GooglePubsubConfig;
import com.realkinetic.app.gabby.repository.BaseDownstream;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import org.junit.BeforeClass;

import java.io.IOException;
import java.util.logging.Logger;

public class GooglePubsubDownstreamTest extends BaseDownstream {
    private static final Logger LOG = Logger.getLogger(GooglePubsubDownstreamTest.class.getName());
    private static GooglePubsubDownstream pubsubDownstream;

    @BeforeClass
    public static void beforeClass() throws IOException {
        final BaseConfig config = (BaseConfig) DefaultConfig.load();
        config.setDownstreamTimeout(0);
        final GooglePubsubConfig pubsubConfig = new GooglePubsubConfig();
        pubsubConfig.setAppName("gabby");
        pubsubConfig.setProject("static-files-rk");
        config.setGooglePubsubConfig(pubsubConfig);
        pubsubDownstream = new GooglePubsubDownstream(config);
    }

    @Override
    protected DownstreamSubscription getDownstream() {
        return pubsubDownstream;
    }

    @Override
    public void testPullDeadLetterMaxAccesses() {
        // this feature isn't supported in google pub sub
    }
}