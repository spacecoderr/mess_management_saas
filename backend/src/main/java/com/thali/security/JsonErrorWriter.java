package com.thali.security;

import java.io.IOException;
import java.time.Instant;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.http.HttpServletResponse;

/** Writes the same problem+json shape as GlobalExceptionHandler for errors raised inside the security filter chain. */
final class JsonErrorWriter {

    private JsonErrorWriter() {}

    static void write(HttpServletResponse res, HttpStatus status, String code, String detail) throws IOException {
        res.setStatus(status.value());
        res.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        res.getWriter().write("{\"type\":\"about:blank\",\"title\":\"" + status.getReasonPhrase() + "\",\"status\":" + status.value()
                + ",\"detail\":\"" + detail + "\",\"code\":\"" + code + "\",\"timestamp\":\"" + Instant.now() + "\"}");
    }
}
