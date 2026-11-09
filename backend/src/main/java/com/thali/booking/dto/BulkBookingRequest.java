package com.thali.booking.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

public record BulkBookingRequest(@NotEmpty @Size(max = 60) List<@Valid BookingRequest> items) {}
