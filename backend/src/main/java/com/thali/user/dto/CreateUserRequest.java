package com.thali.user.dto;

import com.thali.user.Role;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Admin creates STAFF (or another ADMIN). Students always self-register with their college email. */
public record CreateUserRequest(
        @NotBlank @Size(max = 120) String name,
        @NotBlank @Email @Size(max = 160) String email,
        @Pattern(regexp = "^[0-9+\\- ]{10,15}$", message = "Enter a valid phone number") String phone,
        @NotNull Role role,
        @NotBlank @Pattern(regexp = "^(?=.*[A-Za-z])(?=.*\\d).{8,72}$",
                message = "Password must be 8-72 characters with at least one letter and one number") String password) {}
