package com.thali.config;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** One clock in the campus time zone, injected everywhere so "today" and cutoffs are consistent (and testable). */
@Configuration
public class ClockConfig {

    @Bean
    Clock clock(AppProperties props) {
        return Clock.system(ZoneId.of(props.zone()));
    }
}
