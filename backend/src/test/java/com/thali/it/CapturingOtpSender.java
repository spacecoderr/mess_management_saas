package com.thali.it;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import com.thali.auth.OtpPurpose;
import com.thali.auth.otp.OtpSender;

/** Replaces the console/Brevo sender in tests so a test can read the code a "user" would have received. */
public class CapturingOtpSender implements OtpSender {

    private final Map<String, String> last = new ConcurrentHashMap<>();

    @Override
    public void send(String email, String code, OtpPurpose purpose, int ttlMinutes) {
        last.put(key(email, purpose), code);
    }

    public String lastCode(String email, OtpPurpose purpose) {
        String code = last.get(key(email, purpose));
        if (code == null) throw new IllegalStateException("No OTP was sent to " + email + " for " + purpose);
        return code;
    }

    public boolean wasSent(String email, OtpPurpose purpose) {
        return last.containsKey(key(email, purpose));
    }

    public void clear() {
        last.clear();
    }

    private static String key(String email, OtpPurpose purpose) {
        return email + "|" + purpose;
    }
}
