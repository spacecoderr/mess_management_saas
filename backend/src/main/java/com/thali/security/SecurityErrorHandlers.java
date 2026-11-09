package com.thali.security;

import org.springframework.http.HttpStatus;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.security.web.access.AccessDeniedHandler;

/** 401 / 403 responses for requests rejected by the security filter chain. */
public final class SecurityErrorHandlers {

    private SecurityErrorHandlers() {}

    public static final AuthenticationEntryPoint ENTRY_POINT = (req, res, ex) ->
            JsonErrorWriter.write(res, HttpStatus.UNAUTHORIZED, "UNAUTHENTICATED", "Please log in to continue.");

    public static final AccessDeniedHandler ACCESS_DENIED = (req, res, ex) ->
            JsonErrorWriter.write(res, HttpStatus.FORBIDDEN, "ACCESS_DENIED", "You do not have permission to do that.");
}
