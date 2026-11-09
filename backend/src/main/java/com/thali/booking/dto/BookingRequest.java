package com.thali.booking.dto;

import java.time.LocalDate;

import com.thali.menu.MealType;

import jakarta.validation.constraints.NotNull;

public record BookingRequest(@NotNull LocalDate date, @NotNull MealType mealType) {}
