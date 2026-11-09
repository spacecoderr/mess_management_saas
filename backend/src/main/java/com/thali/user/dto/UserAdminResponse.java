package com.thali.user.dto;

import java.time.Instant;

import com.thali.user.Role;
import com.thali.user.User;

public record UserAdminResponse(Long id, String name, String email, Role role, String phone, boolean emailVerified,
                                boolean active, Instant createdAt) {
    public static UserAdminResponse of(User u) {
        return new UserAdminResponse(u.getId(), u.getName(), u.getEmail(), u.getRole(), u.getPhone(),
                u.isEmailVerified(), u.isActive(), u.getCreatedAt());
    }
}
