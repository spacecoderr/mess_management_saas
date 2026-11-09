package com.thali.auth.dto;

import com.thali.user.DietPref;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank String registrationToken,
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Pattern(regexp = "^[0-9+\\- ]{10,15}$", message = "Enter a valid phone number") String phone,
        @NotBlank @Size(max = 30) String hostelBlock,
        @NotBlank @Size(max = 20) String roomNo,
        @NotNull DietPref dietPref,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "Password must be 8-72 characters with at least one letter and one number") String password) {}
