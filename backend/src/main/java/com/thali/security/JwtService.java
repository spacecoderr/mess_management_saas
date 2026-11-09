package com.thali.security;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.security.oauth2.core.DelegatingOAuth2TokenValidator;
import org.springframework.stereotype.Service;

import com.nimbusds.jose.jwk.source.ImmutableSecret;
import com.thali.common.exception.ApiException;
import com.thali.config.AppProperties;
import com.thali.user.User;

/**
 * Issues and verifies HS256 JWTs. Two kinds exist and are told apart by the "use" claim so one can never be
 * replayed as the other: "access" (API calls) and "registration" (proof the email's OTP was verified).
 */
@Service
public class JwtService {

    public static final String USE_ACCESS = "access";
    public static final String USE_REGISTRATION = "registration";

    private final JwtEncoder encoder;
    private final NimbusJwtDecoder decoder;
    private final SecretKey key;
    private final AppProperties.Jwt props;

    public JwtService(AppProperties appProperties) {
        this.props = appProperties.jwt();
        this.key = new SecretKeySpec(props.secret().getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        this.encoder = new NimbusJwtEncoder(new ImmutableSecret<>(key));
        this.decoder = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
    }

    public long accessTtlSeconds() {
        return props.accessTtlMinutes() * 60L;
    }

    public String issueAccessToken(User user) {
        Instant now = Instant.now();
        return encode(JwtClaimsSet.builder()
                .subject(String.valueOf(user.getId()))
                .claim("role", user.getRole().name())
                .claim("use", USE_ACCESS)
                .issuedAt(now)
                .expiresAt(now.plusSeconds(accessTtlSeconds()))
                .build());
    }

    public String issueRegistrationToken(String email) {
        Instant now = Instant.now();
        return encode(JwtClaimsSet.builder()
                .subject(email)
                .claim("use", USE_REGISTRATION)
                .issuedAt(now)
                .expiresAt(now.plus(props.registrationTtlMinutes(), ChronoUnit.MINUTES))
                .build());
    }

    /** Returns the verified email, or throws 400 if the token is invalid/expired/wrong kind. */
    public String verifyRegistrationToken(String token) {
        try {
            Jwt jwt = decoder.decode(token);
            if (!USE_REGISTRATION.equals(jwt.getClaimAsString("use"))) throw new JwtException("wrong token kind");
            return jwt.getSubject();
        } catch (JwtException e) {
            throw ApiException.badRequest("REGISTRATION_TOKEN_INVALID",
                    "Your email verification has expired. Please verify your email again.");
        }
    }

    /** Decoder used by Spring Security for the Authorization header: only "access" tokens pass. */
    public JwtDecoder accessTokenDecoder() {
        OAuth2TokenValidator<Jwt> accessOnly = jwt -> USE_ACCESS.equals(jwt.getClaimAsString("use"))
                ? OAuth2TokenValidatorResult.success()
                : OAuth2TokenValidatorResult.failure(new OAuth2Error("invalid_token", "Not an access token", null));
        NimbusJwtDecoder d = NimbusJwtDecoder.withSecretKey(key).macAlgorithm(MacAlgorithm.HS256).build();
        d.setJwtValidator(new DelegatingOAuth2TokenValidator<>(JwtValidators.createDefault(), accessOnly));
        return d;
    }

    private String encode(JwtClaimsSet claims) {
        return encoder.encode(JwtEncoderParameters.from(JwsHeader.with(MacAlgorithm.HS256).build(), claims)).getTokenValue();
    }
}
