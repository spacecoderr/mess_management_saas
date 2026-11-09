package com.thali.booking.dto;

import java.time.LocalDate;
import java.util.List;

import com.thali.menu.MealType;

/** Bulk booking is best-effort: what worked is in `booked`, what didn't (and why) is in `skipped`. */
public record BulkBookingResponse(List<BookingResponse> booked, List<Skipped> skipped) {
    public record Skipped(LocalDate date, MealType mealType, String code, String reason) {}
}
