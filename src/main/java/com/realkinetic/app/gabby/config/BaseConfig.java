package com.realkinetic.app.gabby.config;

import java.util.List;

public class BaseConfig implements Config {
    @Override
    public List<String> getTopics() {
        return topics;
    }

    public void setTopics(List<String> topics) {
        this.topics = topics;
    }

    private List<String> topics;
}
