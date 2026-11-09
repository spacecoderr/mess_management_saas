package com.thali.user.dto;

import com.thali.user.DietPref;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Student-only fields are ignored for staff/admin; null student fields mean "leave unchanged". */
public record UpdateProfileRequest(
        @NotBlank @Size(max = 120) String name,
        @Pattern(regexp = "^[0-9+\\- ]{10,15}$", message = "Enter a valid phone number") String phone,
        @Size(max = 30) String hostelBlock,
        @Size(max = 20) String roomNo,
        DietPref dietPref,
        @Min(2000) @Max(2100) Integer batchYear) {}
