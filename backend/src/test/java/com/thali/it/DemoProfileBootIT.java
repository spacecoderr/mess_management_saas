package com.thali.it;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.postgresql.PostgreSQLContainer;

import com.thali.auth.otp.BrevoEmailOtpSender;
import com.thali.auth.otp.OtpSender;

/** Boots the whole app with the production ("demo") profile, which the other tests never use. This caught a startup crash on Render. */
@SpringBootTest
@ActiveProfiles("demo")
class DemoProfileBootIT {

    private static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void props(DynamicPropertyRegistry r) {
        r.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        r.add("spring.datasource.username", POSTGRES::getUsername);
        r.add("spring.datasource.password", POSTGRES::getPassword);
        r.add("app.seed.enabled", () -> "false");
        r.add("app.scheduling.enabled", () -> "false");
    }

    @Autowired OtpSender sender;

    @Test
    void theAppStartsAndUsesTheBrevoSender() {
        assertThat(sender).isInstanceOf(BrevoEmailOtpSender.class);
    }
}
