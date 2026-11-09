package com.thali.it;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Primary;

import com.thali.support.MutableClock;

@TestConfiguration
public class IntegrationTestConfig {

    /** @Primary so every service that injects a Clock gets the movable one. */
    @Bean
    @Primary
    MutableClock testClock() {
        return new MutableClock();
    }

    @Bean
    @Primary
    CapturingOtpSender capturingOtpSender() {
        return new CapturingOtpSender();
    }
}
