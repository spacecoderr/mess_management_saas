package com.thali.attendance.dto;

import java.time.LocalDate;

import com.thali.menu.MealType;

/** booked = everyone with a live booking (served + remaining + no-show); remaining = still expected at the counter. */
public record LiveCountsResponse(LocalDate date, MealType mealType, Long menuId, long booked, long served, long remaining,
                                 long noShow, long cancelled) {}
