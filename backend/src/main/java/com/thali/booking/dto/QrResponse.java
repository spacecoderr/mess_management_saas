package com.thali.booking.dto;

import java.time.LocalDate;
import java.util.UUID;

import com.thali.booking.BookingStatus;
import com.thali.menu.MealType;

/** `payload` is exactly what the QR image should encode; staff scan it into POST /attendance/scan. */
public record QrResponse(Long bookingId, UUID qrToken, String payload, BookingStatus status, LocalDate date, MealType mealType) {}
