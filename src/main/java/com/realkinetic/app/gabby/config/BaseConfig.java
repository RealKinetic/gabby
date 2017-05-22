/*
Copyright 2017 Real Kinetic LLC

Licensed under the Apache License, Version 2.0 (the "License"); you may not use
this file except in compliance with the License. You may obtain a copy of the
License at

http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software distributed
under the License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR
CONDITIONS OF ANY KIND, either express or implied. See the License for the
specific language governing permissions and limitations under the License.
*/
package com.realkinetic.app.gabby.config;

import java.util.ArrayList;
import java.util.Collections;
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

    @Override
    public String getDownstream() {
        return downstream;
    }

    public void setDownstream(String downstream) {
        this.downstream = downstream;
    }

    @Override
    public RedisConfig getRedisConfig() {
        return this.redisConfig;
    }

    public void setRedisConfig(final RedisConfig redisConfig) {
        this.redisConfig = redisConfig;
    }

    public GooglePubsubConfig getGooglePubsubConfig() {
        return googlePubsubConfig;
    }

    public void setGooglePubsubConfig(GooglePubsubConfig googlePubsubConfig) {
        this.googlePubsubConfig = googlePubsubConfig;
    }

    public List<String> validate() {
        List<String> errors = new ArrayList<>();
        if (this.downstream == null || this.downstream.isEmpty()) {
            return Collections.singletonList("must contain a downstream provider");
        } else {
            String downstream = this.downstream.toLowerCase().trim();
            switch (downstream) {
                case "redis":
                    if (this.redisConfig == null) {
                        errors.add("must provide redis configuration");
                    } else {
                        errors.addAll(this.redisConfig.validate());
                    }
                    break;
                case "googlepubsub":
                    if (this.googlePubsubConfig == null) {
                        errors.add("must provide google pubsub configuration");
                    } else {
                        errors.addAll(this.googlePubsubConfig.validate());
                    }
                case "memory":
                    break; // no real configuration right now
                default:
                    errors.add(this.downstream + " is not a valid downstream provider");
            }
        }

        errors.addAll(ConfigUtil.validateParameterGreaterThanZero(downstreamTimeout, "downstreamTimeout"));
        errors.addAll(ConfigUtil.validateParameterGreaterThanZero(upstreamTimeout, "upstreamTimeout"));
        return errors;
    }

    private List<String> topics;
    private int downstreamTimeout;
    private int upstreamTimeout;
    private String downstream;
    private RedisConfig redisConfig;
    private GooglePubsubConfig googlePubsubConfig;
}
