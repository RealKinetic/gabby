package com.realkinetic.app.gabby.config;

import java.util.Arrays;
import java.util.List;

public class DefaultConfig {
    private static final List<String> TOPICS = Arrays.asList("realkinetic", "entities");
    private static final int DOWNSTREAM_TIMEOUT = 30;
    private static final int UPSTREAM_TIMEOUT = 90;

    public static Config load() {
        BaseConfig config = new BaseConfig();
        config.setTopics(TOPICS);
        config.setDownstreamTimeout(DOWNSTREAM_TIMEOUT);
        config.setUpstreamTimeout(UPSTREAM_TIMEOUT);
        return config;
    }
}
