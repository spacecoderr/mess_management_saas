package com.thali.feedback.dto;

import java.util.List;
import java.util.Map;

import com.thali.menu.MealType;

public record FeedbackSummaryResponse(Double averageRating, long totalRatings, List<MealAverage> byMeal, Map<Integer, Long> distribution) {
    public record MealAverage(MealType mealType, Double average, long count) {}
}
