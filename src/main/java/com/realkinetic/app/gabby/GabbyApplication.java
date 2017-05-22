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
package com.realkinetic.app.gabby;

import com.realkinetic.app.gabby.config.Config;
import com.realkinetic.app.gabby.config.DefaultConfig;
import com.realkinetic.app.gabby.config.YamlConfig;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.repository.downstream.redis.RedisDownstream;
import com.realkinetic.app.gabby.service.GooglePubSubMessagingService;
import com.realkinetic.app.gabby.service.MessagingService;
import com.realkinetic.app.gabby.service.StandAloneMessagingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.io.FileNotFoundException;
import java.io.IOException;

@SpringBootApplication
public class GabbyApplication {
    private static final Object LOCK = new Object();
    private static volatile Config CONFIG;

    public static void main(String[] args) {
        SpringApplication.run(GabbyApplication.class, args);
    }

    @Bean
    public DownstreamSubscription downstreamSubscription() {
        return new RedisDownstream(config());
    }

    @Bean
    public MessagingService partnerService() throws IOException {
        return new StandAloneMessagingService(downstreamSubscription());
    }

    @Bean // so this can be autowired
    public Config config() {
        Config config = CONFIG;
        if (config == null) {
            synchronized (LOCK) {
                config = CONFIG;
                if (config == null) {
                    try {
                        config = YamlConfig.load();
                    } catch (FileNotFoundException e) {
                        config = DefaultConfig.load();
                    }
                    CONFIG = config;
                }
            }
        }
        return config;
    }
}
