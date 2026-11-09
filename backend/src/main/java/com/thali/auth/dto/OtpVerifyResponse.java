package com.thali.auth.dto;

/** REGISTER purpose -> registrationToken is set. LOGIN purpose -> auth is set. */
public record OtpVerifyResponse(String registrationToken, AuthResponse auth) {}
