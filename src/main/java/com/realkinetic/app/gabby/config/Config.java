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

import java.util.List;

public interface Config {
    List<String> getTopics();
    // amount of time the http request is open to the client before we timeout, in seconds
    int getClientLongPollingTimeout();
    // amount of time in seconds for upstream acknowledgement timeout
    int getUpstreamTimeout();
    // amount of time in seconds for a downstream acknowledgement timeout
    int getDownstreamTimeout();
    // the type of the downstream provider
    String getDownstream();
    // redis config if configured for redis, if not, this can be null
    RedisConfig getRedisConfig();
    // google pub sub config, if pubsub is not in use this can be null
    GooglePubsubConfig getGooglePubsubConfig();
    // memory config, this may be null if not using memory downstream
    MemoryConfig getMemoryConfig();
    // validates the configuration and returns a list of errors, if the list
    // is empty it's a valid configuration
    List<String> validate();
}
