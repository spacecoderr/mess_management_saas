package com.thali.billing.dto;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.thali.booking.BookingStatus;
import com.thali.menu.MealType;

public record BillLine(LocalDate date, MealType mealType, BookingStatus status, BigDecimal price) {}
