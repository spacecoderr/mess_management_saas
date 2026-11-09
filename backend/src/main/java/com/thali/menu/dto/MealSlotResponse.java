package com.thali.menu.dto;

import java.math.BigDecimal;
import java.time.LocalTime;

import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealType;

public record MealSlotResponse(MealType mealType, LocalTime startTime, LocalTime endTime, Integer cutoffMinutes,
                               BigDecimal price, boolean active) {
    public static MealSlotResponse of(MealSlotConfig c) {
        return new MealSlotResponse(c.getMealType(), c.getStartTime(), c.getEndTime(), c.getCutoffMinutes(), c.getPrice(), c.isActive());
    }
}
