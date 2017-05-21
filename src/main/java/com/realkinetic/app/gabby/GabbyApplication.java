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
