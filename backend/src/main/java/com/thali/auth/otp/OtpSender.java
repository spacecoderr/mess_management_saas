package com.thali.auth.otp;

import com.thali.auth.OtpPurpose;

/** Delivery channel for OTPs. Email now; an SmsOtpSender can be added later without touching OtpService. */
public interface OtpSender {
    void send(String email, String code, OtpPurpose purpose, int ttlMinutes);
}
