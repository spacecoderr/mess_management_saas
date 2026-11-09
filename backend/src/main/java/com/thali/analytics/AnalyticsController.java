package com.thali.analytics;

import java.util.List;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.thali.analytics.AnalyticsDtos.DayPoint;
import com.thali.analytics.AnalyticsDtos.DishScore;
import com.thali.analytics.AnalyticsDtos.MealShare;
import com.thali.analytics.AnalyticsDtos.Overview;
import com.thali.analytics.AnalyticsDtos.RatingPoint;
import com.thali.analytics.AnalyticsDtos.RevenuePoint;
import com.thali.analytics.AnalyticsDtos.Wastage;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "Analytics", description = "Dashboard aggregates. Overview and revenue are admin-only; the rest are open to staff too.")
@PreAuthorize("hasAnyRole('ADMIN','STAFF')")
@RestController
@RequestMapping("/api/v1/analytics")
public class AnalyticsController {

    private final AnalyticsService service;

    public AnalyticsController(AnalyticsService service) {
        this.service = service;
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/overview")
    public Overview overview() {
        return service.overview();
    }

    @GetMapping("/bookings-trend")
    public List<DayPoint> bookingsTrend(@RequestParam(defaultValue = "30") int days) {
        return service.bookingsTrend(days);
    }

    @GetMapping("/meal-distribution")
    public List<MealShare> mealDistribution(@RequestParam(defaultValue = "30") int days) {
        return service.mealDistribution(days);
    }

    @GetMapping("/ratings-trend")
    public List<RatingPoint> ratingsTrend(@RequestParam(defaultValue = "30") int days) {
        return service.ratingsTrend(days);
    }

    @GetMapping("/top-dishes")
    public List<DishScore> topDishes(@RequestParam(defaultValue = "30") int days,
                                     @RequestParam(defaultValue = "5") int limit,
                                     @RequestParam(defaultValue = "best") String order,
                                     @RequestParam(defaultValue = "1") int minRatings) {
        return service.topDishes(days, limit, !"worst".equalsIgnoreCase(order), minRatings);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/revenue")
    public List<RevenuePoint> revenue(@RequestParam(defaultValue = "6") int months) {
        return service.revenue(months);
    }

    @GetMapping("/wastage")
    public Wastage wastage(@RequestParam(defaultValue = "30") int days) {
        return service.wastage(days);
    }
}
