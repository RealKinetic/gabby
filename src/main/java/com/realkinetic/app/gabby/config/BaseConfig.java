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

    @Override
    public int getDownstreamTimeout() {
        return downstreamTimeout;
    }

    public void setDownstreamTimeout(int downstreamTimeout) {
        this.downstreamTimeout = downstreamTimeout;
    }

    @Override
    public int getUpstreamTimeout() {
        return upstreamTimeout;
    }

    public void setUpstreamTimeout(int upstreamTimeout) {
        this.upstreamTimeout = upstreamTimeout;
    }

    private List<String> topics;
    private int downstreamTimeout;
    private int upstreamTimeout;
}
