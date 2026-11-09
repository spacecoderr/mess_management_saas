package com.thali.auth;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import com.thali.auth.dto.AuthResponse;
import com.thali.auth.dto.LoginRequest;
import com.thali.auth.dto.MessageResponse;
import com.thali.auth.dto.OtpRequest;
import com.thali.auth.dto.OtpVerifyRequest;
import com.thali.auth.dto.OtpVerifyResponse;
import com.thali.auth.dto.RefreshRequest;
import com.thali.auth.dto.RegisterRequest;
import com.thali.auth.dto.ResetPasswordRequest;
import com.thali.auth.dto.UserResponse;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@Tag(name = "Auth", description = "College-email OTP verification, registration, login and tokens")
@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService auth;

    public AuthController(AuthService auth) {
        this.auth = auth;
    }

    @Operation(summary = "Send a 6-digit OTP to the college email",
            description = "REGISTER / LOGIN need an @iiit-bh.ac.in student email. In dev the code is printed in the backend log.",
            security = {})
    @PostMapping("/otp/request")
    public MessageResponse requestOtp(@Valid @RequestBody OtpRequest req) {
        auth.requestOtp(req);
        return new MessageResponse("If the email is eligible, a verification code has been sent.");
    }

    @Operation(summary = "Verify an OTP", description = "REGISTER returns a registrationToken; LOGIN returns access + refresh tokens.", security = {})
    @PostMapping("/otp/verify")
    public OtpVerifyResponse verifyOtp(@Valid @RequestBody OtpVerifyRequest req) {
        return auth.verifyOtp(req);
    }

    @Operation(summary = "Create a student account after email verification (waits for admin approval)", security = {})
    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    public UserResponse register(@Valid @RequestBody RegisterRequest req) {
        return auth.register(req);
    }

    @Operation(summary = "Log in with email and password", security = {})
    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest req) {
        return auth.login(req);
    }

    @Operation(summary = "Reset password using an OTP sent with purpose RESET_PASSWORD", security = {})
    @PostMapping("/password/reset")
    public MessageResponse resetPassword(@Valid @RequestBody ResetPasswordRequest req) {
        auth.resetPassword(req);
        return new MessageResponse("Password updated. Please log in with your new password.");
    }

    @Operation(summary = "Exchange a refresh token for a new access + refresh token (rotation)", security = {})
    @PostMapping("/refresh")
    public AuthResponse refresh(@Valid @RequestBody RefreshRequest req) {
        return auth.refresh(req.refreshToken());
    }

    @Operation(summary = "Revoke a refresh token", security = {})
    @PostMapping("/logout")
    public MessageResponse logout(@Valid @RequestBody RefreshRequest req) {
        auth.logout(req.refreshToken());
        return new MessageResponse("Logged out.");
    }

    @Operation(summary = "Current user", security = @SecurityRequirement(name = "bearerAuth"))
    @GetMapping("/me")
    public UserResponse me(@AuthenticationPrincipal Jwt jwt) {
        return auth.me(Long.valueOf(jwt.getSubject()));
    }
}
