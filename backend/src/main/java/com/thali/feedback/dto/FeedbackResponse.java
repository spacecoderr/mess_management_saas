package com.thali.feedback.dto;

import java.time.Instant;
import java.time.LocalDate;

import com.thali.feedback.Feedback;
import com.thali.menu.MealType;

/** studentName is only set in staff/admin views. */
public record FeedbackResponse(Long id, Long dailyMenuId, LocalDate date, MealType mealType, int rating, String comment,
                               Instant createdAt, String studentName) {
    public static FeedbackResponse of(Feedback f, boolean withStudent) {
        var m = f.getDailyMenu();
        return new FeedbackResponse(f.getId(), m.getId(), m.getMenuDate(), m.getMealType(), f.getRating(), f.getComment(),
                f.getCreatedAt(), withStudent ? f.getStudent().getName() : null);
    }
}
