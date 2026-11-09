package com.thali.attendance.dto;

import jakarta.validation.constraints.NotNull;

public record ManualRequest(@NotNull Long bookingId) {}
