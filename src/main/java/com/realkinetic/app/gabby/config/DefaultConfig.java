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
