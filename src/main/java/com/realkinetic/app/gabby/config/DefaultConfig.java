package com.realkinetic.app.gabby.config;

import java.util.Arrays;
import java.util.List;

public class DefaultConfig {
    private static final List<String> TOPICS = Arrays.asList("realkinetic", "entities");

    public static Config load() {
        BaseConfig config = new BaseConfig();
        config.setTopics(TOPICS);
        return config;
    }
}
