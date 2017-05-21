package com.realkinetic.app.gabby.config;

import java.util.List;

public interface Config {
    List<String> getTopics();
    // amount of time in seconds for upstream acknowledgement timeout
    int getUpstreamTimeout();
    // amount of time in seconds for a downstream acknowledgement timeout
    int getDownstreamTimeout();
}
