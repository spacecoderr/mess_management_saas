package com.thali.feedback;

import java.time.LocalDate;
import java.util.EnumSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.booking.Booking;
import com.thali.booking.BookingRepository;
import com.thali.booking.BookingStatus;
import com.thali.common.PageResponse;
import com.thali.common.exception.ApiException;
import com.thali.feedback.dto.FeedbackRequest;
import com.thali.feedback.dto.FeedbackResponse;
import com.thali.feedback.dto.FeedbackSummaryResponse;
import com.thali.feedback.dto.PendingFeedbackResponse;
import com.thali.menu.DailyMenu;
import com.thali.menu.DailyMenuRepository;
import com.thali.menu.MealType;
import com.thali.menu.MenuItem;
import com.thali.user.UserRepository;

/** A student can rate a meal once, and only if they actually ate it (attendance marked CONSUMED). */
@Service
public class FeedbackService {

    private final FeedbackRepository feedback;
    private final BookingRepository bookings;
    private final DailyMenuRepository menus;
    private final UserRepository users;

    public FeedbackService(FeedbackRepository feedback, BookingRepository bookings, DailyMenuRepository menus, UserRepository users) {
        this.feedback = feedback;
        this.bookings = bookings;
        this.menus = menus;
        this.users = users;
    }

    @Transactional
    public FeedbackResponse submit(Long studentId, FeedbackRequest req) {
        DailyMenu menu = menus.findById(req.dailyMenuId())
                .orElseThrow(() -> ApiException.notFound("MENU_NOT_FOUND", "Meal not found."));
        boolean ate = bookings.findByStudentIdAndDailyMenuId(studentId, menu.getId())
                .map(b -> b.getStatus() == BookingStatus.CONSUMED).orElse(false);
        if (!ate) throw ApiException.unprocessable("NOT_EATEN", "You can only rate meals you've eaten.");
        if (feedback.existsByStudentIdAndDailyMenuId(studentId, menu.getId())) {
            throw ApiException.conflict("ALREADY_RATED", "You've already rated this meal.");
        }
        Feedback f = new Feedback();
        f.setStudent(users.getReferenceById(studentId));
        f.setDailyMenu(menu);
        f.setRating(req.rating().shortValue());
        f.setComment(req.comment() == null || req.comment().isBlank() ? null : req.comment().trim());
        return FeedbackResponse.of(feedback.save(f), false);
    }

    @Transactional(readOnly = true)
    public List<FeedbackResponse> mine(Long studentId) {
        return feedback.findMine(studentId).stream().map(f -> FeedbackResponse.of(f, false)).toList();
    }

    @Transactional(readOnly = true)
    public List<PendingFeedbackResponse> pending(Long studentId) {
        return bookings.findUnrated(studentId).stream().map(Booking::getDailyMenu)
                .map(m -> new PendingFeedbackResponse(m.getId(), m.getMenuDate(), m.getMealType(),
                        m.getItems().stream().map(MenuItem::getName).sorted().toList()))
                .toList();
    }

    @Transactional(readOnly = true)
    public PageResponse<FeedbackResponse> search(LocalDate from, LocalDate to, MealType type, int page, int size) {
        var types = type == null ? EnumSet.allOf(MealType.class) : EnumSet.of(type);
        var result = feedback.search(from, to, types, PageRequest.of(page, Math.max(1, Math.min(size, 100)), Sort.by("createdAt").descending()));
        return PageResponse.of(result, f -> FeedbackResponse.of(f, true));
    }

    @Transactional(readOnly = true)
    public FeedbackSummaryResponse summary(LocalDate from, LocalDate to) {
        long total = 0;
        double weighted = 0;
        List<FeedbackSummaryResponse.MealAverage> byMeal = new java.util.ArrayList<>();
        for (Object[] r : feedback.averageByMeal(from, to)) {
            double avg = ((Number) r[1]).doubleValue();
            long count = ((Number) r[2]).longValue();
            total += count;
            weighted += avg * count;
            byMeal.add(new FeedbackSummaryResponse.MealAverage((MealType) r[0], round(avg), count));
        }
        byMeal.sort(java.util.Comparator.comparing(FeedbackSummaryResponse.MealAverage::mealType));
        Map<Integer, Long> dist = new LinkedHashMap<>();
        for (int i = 1; i <= 5; i++) dist.put(i, 0L);
        for (Object[] r : feedback.distribution(from, to)) dist.put(((Number) r[0]).intValue(), ((Number) r[1]).longValue());
        return new FeedbackSummaryResponse(total == 0 ? null : round(weighted / total), total, byMeal, dist);
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }
}
