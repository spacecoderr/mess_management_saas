package com.thali.auth.dto;

import com.thali.auth.OtpPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;

public record OtpVerifyRequest(
        @NotBlank @Email String email,
        @NotNull OtpPurpose purpose,
        @NotBlank @Pattern(regexp = "\\d{6}", message = "Code must be 6 digits") String code) {}
