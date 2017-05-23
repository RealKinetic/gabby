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
import com.realkinetic.app.gabby.config.YamlConfig;
import com.realkinetic.app.gabby.model.error.InvalidConfigurationException;
import com.realkinetic.app.gabby.repository.DownstreamSubscription;
import com.realkinetic.app.gabby.repository.downstream.google.pubsub.GooglePubsubDownstream;
import com.realkinetic.app.gabby.repository.downstream.memory.MemoryDownstream;
import com.realkinetic.app.gabby.repository.downstream.redis.RedisDownstream;
import com.realkinetic.app.gabby.service.MessagingService;
import com.realkinetic.app.gabby.service.StandAloneMessagingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import springfox.documentation.builders.PathSelectors;
import springfox.documentation.builders.RequestHandlerSelectors;
import springfox.documentation.spi.DocumentationType;
import springfox.documentation.spring.web.plugins.Docket;
import springfox.documentation.swagger2.annotations.EnableSwagger2;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

@SpringBootApplication
@EnableSwagger2
public class GabbyApplication {
    private static final Logger LOG = Logger.getLogger(GabbyApplication.class.getName());
    private static final Object LOCK = new Object();
    private static volatile Config CONFIG;

    public static void main(String[] args) {
        SpringApplication.run(GabbyApplication.class, args);
    }

    @Bean
    public DownstreamSubscription downstreamSubscription() throws IOException, InvalidConfigurationException {
        final Config config = this.config();
        switch (config.getDownstream()) {
            case "redis":
                return new RedisDownstream(config);
            case "googlepubsub":
                return new GooglePubsubDownstream(config);
            case "memory":
                return new MemoryDownstream(config);
            default:
                throw new InvalidConfigurationException(Collections.singletonList(config.getDownstream() + " not recognized"));
        }
    }

    @Bean
    public MessagingService partnerService() throws IOException, InvalidConfigurationException {
        return new StandAloneMessagingService(downstreamSubscription());
    }

    @Bean // so this can be autowired
    public Config config() throws IOException, InvalidConfigurationException {
        Config config = CONFIG;
        if (config == null) {
            synchronized (LOCK) {
                config = CONFIG;
                if (config == null) {
                    config = YamlConfig.load();
                    List<String> errors = config.validate();
                    if (errors != null && errors.size() > 0) {
                        errors.forEach(message -> {
                            LOG.severe(message + "\n");
                        });
                        throw new InvalidConfigurationException(errors);
                    }
                    CONFIG = config;
                }
            }
        }
        return config;
    }

    @Bean
    public Docket api() {
        return new Docket(DocumentationType.SWAGGER_2)
                .select()
                .apis(RequestHandlerSelectors.any())
                .paths(PathSelectors.any())
                .build();
    }
}
