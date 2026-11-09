package com.thali.auth.otp;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

import com.thali.auth.OtpPurpose;

/** Local development: prints the OTP in the backend log instead of sending email. */
@Component
@Profile("!demo")
public class ConsoleOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(ConsoleOtpSender.class);

    @Override
    public void send(String email, String code, OtpPurpose purpose, int ttlMinutes) {
        log.info("==== OTP [{}] for {} -> {} (valid {} min) ====", purpose, email, code, ttlMinutes);
    }
}
