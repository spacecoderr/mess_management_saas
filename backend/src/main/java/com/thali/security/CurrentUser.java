package com.thali.security;

import org.springframework.security.oauth2.jwt.Jwt;

import com.thali.user.Role;

/** Small helpers for reading the authenticated user from the JWT principal. */
public final class CurrentUser {

    private CurrentUser() {}

    public static Long id(Jwt jwt) {
        return Long.valueOf(jwt.getSubject());
    }

    public static Role role(Jwt jwt) {
        return Role.valueOf(jwt.getClaimAsString("role"));
    }
}
