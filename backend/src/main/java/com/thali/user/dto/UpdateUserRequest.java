package com.thali.user.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "^[0-9+\\- ]{10,15}$", message = "Enter a valid phone number") String phone) {}
