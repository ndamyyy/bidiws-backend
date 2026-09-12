package com.bidiws.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

// Clock injectable plutot que Instant.now()/LocalDateTime.now() en dur :
// permet de tester des logiques dependantes du temps (ex. IpRateLimiter)
// avec un temps controle, sans Thread.sleep ni horloge reelle.
@Configuration
public class ClockConfig {

    @Bean
    public Clock clock() {
        return Clock.systemUTC();
    }
}
