package com.thali.attendance.dto;

import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record ScanRequest(@NotNull UUID qrToken) {}
