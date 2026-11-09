package com.thali.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Base64;
import java.util.HexFormat;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.auth.dto.AuthResponse;
import com.thali.auth.dto.LoginRequest;
import com.thali.auth.dto.OtpRequest;
import com.thali.auth.dto.OtpVerifyRequest;
import com.thali.auth.dto.OtpVerifyResponse;
import com.thali.auth.dto.RegisterRequest;
import com.thali.auth.dto.ResetPasswordRequest;
import com.thali.auth.dto.UserResponse;
import com.thali.auth.otp.OtpService;
import com.thali.common.exception.ApiException;
import com.thali.config.AppProperties;
import com.thali.security.JwtService;
import com.thali.user.ApprovalStatus;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;
import com.thali.user.UserRepository;

@Service
public class AuthService {

    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final RefreshTokenRepository refreshTokens;
    private final OtpService otp;
    private final EmailPolicy emailPolicy;
    private final JwtService jwt;
    private final PasswordEncoder encoder;
    private final AppProperties.Jwt jwtCfg;
    private final SecureRandom random = new SecureRandom();

    public AuthService(UserRepository users, StudentProfileRepository profiles, RefreshTokenRepository refreshTokens,
                       OtpService otp, EmailPolicy emailPolicy, JwtService jwt, PasswordEncoder encoder, AppProperties props) {
        this.users = users;
        this.profiles = profiles;
        this.refreshTokens = refreshTokens;
        this.otp = otp;
        this.emailPolicy = emailPolicy;
        this.jwt = jwt;
        this.encoder = encoder;
        this.jwtCfg = props.jwt();
    }

    // ------------------------------------------------------------------ OTP

    @Transactional
    public void requestOtp(OtpRequest req) {
        String email = emailPolicy.normalize(req.email());
        switch (req.purpose()) {
            case REGISTER -> {
                requireCollegeEmail(email);
                if (users.existsByEmailIgnoreCase(email)) {
                    throw ApiException.conflict("EMAIL_ALREADY_REGISTERED", "This email is already registered. Try logging in.");
                }
                otp.issue(email, OtpPurpose.REGISTER);
            }
            case LOGIN -> {
                requireCollegeEmail(email);
                // Unknown / disabled accounts get the same success response so emails can't be probed.
                if (users.findByEmailIgnoreCase(email).filter(User::isActive).isPresent()) otp.issue(email, OtpPurpose.LOGIN);
            }
            case RESET_PASSWORD -> {
                if (users.findByEmailIgnoreCase(email).filter(User::isActive).isPresent()) otp.issue(email, OtpPurpose.RESET_PASSWORD);
            }
        }
    }

    @Transactional(noRollbackFor = ApiException.class)
    public OtpVerifyResponse verifyOtp(OtpVerifyRequest req) {
        String email = emailPolicy.normalize(req.email());
        if (req.purpose() == OtpPurpose.RESET_PASSWORD) {
            throw ApiException.badRequest("UNSUPPORTED_PURPOSE", "Use the password reset endpoint for this purpose.");
        }
        otp.verifyAndConsume(email, req.purpose(), req.code());

        if (req.purpose() == OtpPurpose.REGISTER) {
            return new OtpVerifyResponse(jwt.issueRegistrationToken(email), null);
        }
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Invalid credentials."));
        return new OtpVerifyResponse(null, startSession(user));
    }

    // ------------------------------------------------------------ register

    @Transactional
    public UserResponse register(RegisterRequest req) {
        String email = jwt.verifyRegistrationToken(req.registrationToken());
        if (users.existsByEmailIgnoreCase(email)) {
            throw ApiException.conflict("EMAIL_ALREADY_REGISTERED", "This email is already registered. Try logging in.");
        }
        String rollNo = emailPolicy.rollNo(email);
        if (profiles.existsByRollNo(rollNo)) {
            throw ApiException.conflict("ROLL_NO_TAKEN", "A student with this roll number already exists.");
        }

        User user = new User();
        user.setName(req.name().trim());
        user.setEmail(email);
        user.setPasswordHash(encoder.encode(req.password()));
        user.setRole(Role.STUDENT);
        user.setPhone(req.phone().trim());
        user.setEmailVerified(true);
        user = users.save(user);

        StudentProfile profile = new StudentProfile();
        profile.setUser(user);
        profile.setRollNo(rollNo);
        profile.setBatchYear(emailPolicy.batchYear(rollNo));
        profile.setHostelBlock(req.hostelBlock().trim());
        profile.setRoomNo(req.roomNo().trim());
        profile.setDietPref(req.dietPref());
        profile.setApprovalStatus(ApprovalStatus.PENDING);
        profiles.save(profile);

        return UserResponse.of(user, profile);
    }

    // --------------------------------------------------------------- login

    @Transactional
    public AuthResponse login(LoginRequest req) {
        User user = users.findByEmailIgnoreCase(emailPolicy.normalize(req.email()))
                .filter(u -> encoder.matches(req.password(), u.getPasswordHash()))
                .orElseThrow(() -> ApiException.unauthorized("INVALID_CREDENTIALS", "Incorrect email or password."));
        return startSession(user);
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest req) {
        String email = emailPolicy.normalize(req.email());
        otp.verifyAndConsume(email, OtpPurpose.RESET_PASSWORD, req.code());
        User user = users.findByEmailIgnoreCase(email)
                .orElseThrow(() -> ApiException.badRequest("OTP_INVALID", "That code is invalid or has expired."));
        user.setPasswordHash(encoder.encode(req.newPassword()));
        refreshTokens.revokeAllForUser(user.getId());   // log out everywhere
    }

    // ------------------------------------------------------------- tokens

    @Transactional
    public AuthResponse refresh(String rawToken) {
        RefreshToken stored = refreshTokens.findByTokenHash(sha256(rawToken))
                .filter(t -> !t.isRevoked() && t.getExpiresAt().isAfter(Instant.now()))
                .orElseThrow(() -> ApiException.unauthorized("REFRESH_TOKEN_INVALID", "Your session has expired. Please log in again."));
        stored.setRevoked(true);   // rotation: every refresh token is single-use
        return startSession(stored.getUser());
    }

    @Transactional
    public void logout(String rawToken) {
        refreshTokens.findByTokenHash(sha256(rawToken)).ifPresent(t -> t.setRevoked(true));
    }

    @Transactional(readOnly = true)
    public UserResponse me(Long userId) {
        User user = users.findById(userId)
                .orElseThrow(() -> ApiException.unauthorized("UNAUTHENTICATED", "Please log in to continue."));
        return UserResponse.of(user, profiles.findById(userId).orElse(null));
    }

    // ------------------------------------------------------------ helpers

    /** Final gate before any session is created: active account, and students must be approved. */
    private AuthResponse startSession(User user) {
        if (!user.isActive()) {
            throw ApiException.forbidden("ACCOUNT_DISABLED", "This account has been disabled. Contact the mess office.");
        }
        StudentProfile profile = user.getRole() == Role.STUDENT ? profiles.findById(user.getId()).orElse(null) : null;
        if (profile != null && profile.getApprovalStatus() == ApprovalStatus.PENDING) {
            throw ApiException.forbidden("ACCOUNT_PENDING_APPROVAL", "Your account is waiting for admin approval.");
        }
        if (profile != null && profile.getApprovalStatus() == ApprovalStatus.REJECTED) {
            throw ApiException.forbidden("ACCOUNT_REJECTED", "Your registration was not approved. Contact the mess office.");
        }

        byte[] bytes = new byte[32];
        random.nextBytes(bytes);
        String rawRefresh = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken rt = new RefreshToken();
        rt.setUser(user);
        rt.setTokenHash(sha256(rawRefresh));
        rt.setExpiresAt(Instant.now().plus(jwtCfg.refreshTtlDays(), ChronoUnit.DAYS));
        refreshTokens.save(rt);

        return new AuthResponse(jwt.issueAccessToken(user), rawRefresh, "Bearer", jwt.accessTtlSeconds(), UserResponse.of(user, profile));
    }

    private void requireCollegeEmail(String email) {
        if (!emailPolicy.isCollegeStudentEmail(email)) {
            throw ApiException.badRequest("INVALID_COLLEGE_EMAIL",
                    "Use your IIIT Bhubaneswar email, e.g. b423046@iiit-bh.ac.in");
        }
    }

    private static String sha256(String value) {
        try {
            return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
