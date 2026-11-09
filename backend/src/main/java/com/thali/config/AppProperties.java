package com.thali.config;

import java.math.BigDecimal;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Typed view of the `app.*` block in application.yml. */
@ConfigurationProperties(prefix = "app")
public record AppProperties(String zone, Auth auth, Jwt jwt, Mail mail, Bootstrap bootstrap, Attendance attendance,
                            Billing billing, Seed seed) {

    public record Auth(String allowedDomain, String studentLocalPartRegex, int otpTtlMinutes,
                       int otpMaxAttempts, int otpResendCooldownSeconds, int otpMaxPerHour) {}

    public record Jwt(String secret, int accessTtlMinutes, int refreshTtlDays, int registrationTtlMinutes) {}

    public record Mail(String brevoApiKey, String fromEmail, String fromName) {}

    public record Bootstrap(String adminEmail, String adminPassword) {}

    /** When enforceWindow is false (local demos) meals can be scanned at any time of day. */
    public record Attendance(boolean enforceWindow, int earlyMinutes, int lateMinutes) {}

    /** Bill = consumed + no-show meals + fixedMonthlyCharge; due `dueDays` after the month ends. */
    public record Billing(BigDecimal fixedMonthlyCharge, int dueDays, boolean autoGenerate) {}

    /** Demo data loaded once on dev/demo profiles when enabled. */
    public record Seed(boolean enabled, String demoPassword) {}
}
