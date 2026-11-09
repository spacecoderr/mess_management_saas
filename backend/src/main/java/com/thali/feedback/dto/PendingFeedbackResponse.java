package com.thali.feedback.dto;

import java.time.LocalDate;
import java.util.List;

import com.thali.menu.MealType;

/** A meal the student ate and can still rate. */
public record PendingFeedbackResponse(Long dailyMenuId, LocalDate date, MealType mealType, List<String> dishes) {}
