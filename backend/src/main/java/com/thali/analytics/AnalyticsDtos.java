package com.thali.analytics;

import java.math.BigDecimal;
import java.time.LocalDate;

import com.thali.menu.MealType;

/** Response shapes for /analytics/*. Grouped in one file because they are tiny, read-only views. */
public final class AnalyticsDtos {

    private AnalyticsDtos() {}

    public record Overview(long approvedStudents, long pendingApprovals, long todayBookings, long todayServed,
                           BigDecimal revenueThisMonth, BigDecimal outstanding, long overdueBills, Double averageRating30d,
                           long openComplaints) {}

    public record DayPoint(LocalDate date, long booked, long served, long noShow, long cancelled) {}

    public record MealShare(MealType mealType, long booked, long served, long noShow, long cancelled, double servedSharePercent) {}

    public record RatingPoint(LocalDate date, Double averageRating, long count) {}

    public record DishScore(Long dishId, String name, Double averageRating, long ratings) {}

    public record RevenuePoint(String month, BigDecimal billed, BigDecimal collected, BigDecimal outstanding, long bills, long paidBills) {}

    public record MealWastage(MealType mealType, long resolvedBookings, long noShow, double noShowRatePercent, BigDecimal lostValue) {}

    public record Wastage(long resolvedBookings, long noShow, double noShowRatePercent, BigDecimal lostValue, java.util.List<MealWastage> byMeal) {}
}
