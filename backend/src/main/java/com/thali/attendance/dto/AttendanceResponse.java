package com.thali.attendance.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.thali.attendance.AttendanceMethod;
import com.thali.menu.MealType;
import com.thali.user.DietPref;

public record AttendanceResponse(Long bookingId, String studentName, String rollNo, DietPref dietPref, LocalDate date,
                                 MealType mealType, AttendanceMethod method, Instant markedAt) {}
