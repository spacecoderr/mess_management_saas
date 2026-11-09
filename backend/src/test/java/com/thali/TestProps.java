package com.thali;

import java.math.BigDecimal;

import com.thali.config.AppProperties;

public final class TestProps {
    private TestProps() {}

    public static AppProperties create() {
        return create(true, "300.00");
    }

    public static AppProperties create(boolean enforceWindow, String fixedCharge) {
        return new AppProperties("Asia/Kolkata",
                new AppProperties.Auth("iiit-bh.ac.in", "[a-z]\\d{6}", 5, 5, 60, 5),
                new AppProperties.Jwt("test-secret-test-secret-test-secret-123", 15, 7, 15),
                new AppProperties.Mail("", "", "Thali"),
                new AppProperties.Bootstrap("", ""),
                new AppProperties.Attendance(enforceWindow, 30, 30),
                new AppProperties.Billing(new BigDecimal(fixedCharge), 15, true),
                new AppProperties.Seed(false, "Demo@1234"));
    }
}
