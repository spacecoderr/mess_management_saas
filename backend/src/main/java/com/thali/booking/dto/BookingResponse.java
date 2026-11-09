package com.thali.booking.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

import com.thali.booking.BookingStatus;
import com.thali.menu.MealType;

/** qrToken is only filled for the owning student; student* fields only for staff/admin views. */
public record BookingResponse(Long id, Long dailyMenuId, LocalDate date, MealType mealType, BookingStatus status,
                              BigDecimal price, UUID qrToken, LocalTime startTime, LocalTime endTime,
                              Instant bookingClosesAt, boolean cancellable, List<String> dishes,
                              Long studentId, String studentName, String rollNo) {}
