package com.thali.menu.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;

import com.thali.menu.DailyMenu;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealType;

/** A menu for one meal, plus the slot's timing/price and the moment booking closes. */
public record DailyMenuResponse(Long id, LocalDate date, MealType mealType, boolean published, List<MenuItemResponse> items,
                                LocalTime startTime, LocalTime endTime, BigDecimal price, Instant bookingClosesAt,
                                boolean bookingOpen) {

    public static DailyMenuResponse of(DailyMenu m, MealSlotConfig slot, Instant closesAt, Instant now) {
        return new DailyMenuResponse(m.getId(), m.getMenuDate(), m.getMealType(), m.isPublished(),
                m.getItems().stream().sorted(Comparator.comparing(i -> i.getCategory().ordinal())).map(MenuItemResponse::of).toList(),
                slot.getStartTime(), slot.getEndTime(), slot.getPrice(), closesAt,
                m.isPublished() && slot.isActive() && now.isBefore(closesAt));
    }
}
