package com.thali.auth.dto;

import com.thali.auth.OtpPurpose;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record OtpRequest(@NotBlank @Email String email, @NotNull OtpPurpose purpose) {}
