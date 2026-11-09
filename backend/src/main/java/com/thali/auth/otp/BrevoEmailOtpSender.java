package com.thali.auth.otp;

import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.annotation.Profile;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import com.thali.auth.OtpPurpose;
import com.thali.common.exception.ApiException;
import com.thali.config.AppProperties;

/** Production/demo: sends the OTP through Brevo's HTTPS API (Render's free tier blocks SMTP ports). */
@Component
@Profile("demo")
public class BrevoEmailOtpSender implements OtpSender {

    private static final Logger log = LoggerFactory.getLogger(BrevoEmailOtpSender.class);

    private final RestClient client;
    private final AppProperties.Mail mail;

    public BrevoEmailOtpSender(RestClient.Builder builder, AppProperties props) {
        this.mail = props.mail();
        this.client = builder.baseUrl("https://api.brevo.com/v3").build();
    }

    @Override
    public void send(String email, String code, OtpPurpose purpose, int ttlMinutes) {
        String action = switch (purpose) {
            case REGISTER -> "verify your email and create your Thali account";
            case LOGIN -> "log in to Thali";
            case RESET_PASSWORD -> "reset your Thali password";
        };
        String html = "<div style=\"font-family:Arial,sans-serif;max-width:420px;margin:auto;padding:24px;background:#FBF7EE;"
                + "border-radius:16px;color:#173B2C\"><h2 style=\"margin:0 0 8px\">Thali · IIIT Bhubaneswar Mess</h2>"
                + "<p>Use this code to " + action + ":</p>"
                + "<p style=\"font-size:34px;letter-spacing:8px;font-weight:700;color:#C8643B;margin:16px 0\">" + code + "</p>"
                + "<p style=\"font-size:13px;color:#555\">It expires in " + ttlMinutes + " minutes. If you didn't ask for it, ignore this email.</p></div>";
        Map<String, Object> body = Map.of(
                "sender", Map.of("name", mail.fromName(), "email", mail.fromEmail()),
                "to", List.of(Map.of("email", email)),
                "subject", "Your Thali verification code: " + code,
                "htmlContent", html);
        try {
            client.post().uri("/smtp/email").header("api-key", mail.brevoApiKey()).body(body).retrieve().toBodilessEntity();
        } catch (RestClientException e) {
            log.error("Brevo send failed for {}: {}", email, e.getMessage());
            throw new ApiException(HttpStatus.BAD_GATEWAY, "OTP_DELIVERY_FAILED", "We couldn't send the email right now. Please try again.");
        }
    }
}
