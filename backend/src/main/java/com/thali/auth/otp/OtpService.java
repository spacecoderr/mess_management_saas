package com.thali.auth.otp;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.auth.OtpCode;
import com.thali.auth.OtpCodeRepository;
import com.thali.auth.OtpPurpose;
import com.thali.common.exception.ApiException;
import com.thali.config.AppProperties;

/** Generates, rate-limits and verifies one-time codes. Only a BCrypt hash of each code is stored. */
@Service
public class OtpService {

    private final OtpCodeRepository repo;
    private final OtpSender sender;
    private final PasswordEncoder encoder;
    private final AppProperties.Auth cfg;
    private final SecureRandom random = new SecureRandom();

    public OtpService(OtpCodeRepository repo, OtpSender sender, PasswordEncoder encoder, AppProperties props) {
        this.repo = repo;
        this.sender = sender;
        this.encoder = encoder;
        this.cfg = props.auth();
    }

    @Transactional
    public void issue(String email, OtpPurpose purpose) {
        Instant now = Instant.now();

        repo.findFirstByEmailAndPurposeOrderByCreatedAtDesc(email, purpose).ifPresent(last -> {
            long wait = last.getCreatedAt().plusSeconds(cfg.otpResendCooldownSeconds()).getEpochSecond() - now.getEpochSecond();
            if (wait > 0) throw ApiException.tooMany("OTP_COOLDOWN", "Please wait " + wait + " seconds before requesting another code.");
        });
        if (repo.countByEmailAndCreatedAtAfter(email, now.minus(1, ChronoUnit.HOURS)) >= cfg.otpMaxPerHour()) {
            throw ApiException.tooMany("OTP_HOURLY_LIMIT", "Too many codes requested. Try again in an hour.");
        }

        repo.invalidateActive(email, purpose);
        String code = String.format("%06d", random.nextInt(1_000_000));
        OtpCode otp = new OtpCode();
        otp.setEmail(email);
        otp.setPurpose(purpose);
        otp.setCodeHash(encoder.encode(code));
        otp.setExpiresAt(now.plus(cfg.otpTtlMinutes(), ChronoUnit.MINUTES));
        repo.save(otp);

        sender.send(email, code, purpose, cfg.otpTtlMinutes());   // throws -> whole transaction rolls back
    }

    /** Wrong guesses must still be counted, so ApiException does NOT roll the transaction back. */
    @Transactional(noRollbackFor = ApiException.class)
    public void verifyAndConsume(String email, OtpPurpose purpose, String code) {
        OtpCode otp = repo.findFirstByEmailAndPurposeAndConsumedFalseOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> ApiException.badRequest("OTP_INVALID", "That code is invalid or has expired."));

        if (otp.getExpiresAt().isBefore(Instant.now())) {
            otp.setConsumed(true);
            throw ApiException.badRequest("OTP_INVALID", "That code is invalid or has expired.");
        }
        if (otp.getAttempts() >= cfg.otpMaxAttempts()) {
            otp.setConsumed(true);
            throw ApiException.tooMany("OTP_TOO_MANY_ATTEMPTS", "Too many wrong attempts. Please request a new code.");
        }
        otp.setAttempts(otp.getAttempts() + 1);
        if (!encoder.matches(code, otp.getCodeHash())) {
            throw ApiException.badRequest("OTP_INVALID", "That code is invalid or has expired.");
        }
        otp.setConsumed(true);
    }
}
