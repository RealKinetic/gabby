package com.realkinetic.app.gabby;

import com.realkinetic.app.gabby.service.FirebaseMessagingService;
import com.realkinetic.app.gabby.service.MessagingService;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

@SpringBootApplication
public class GabbyApplication {
    public static void main(String[] args) {
        SpringApplication.run(GabbyApplication.class, args);
    }

    @Bean
    public MessagingService partnerService() {
        return new FirebaseMessagingService();
    }
}
