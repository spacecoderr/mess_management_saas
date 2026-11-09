package com.thali.attendance;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.attendance.dto.AttendanceResponse;
import com.thali.attendance.dto.LiveCountsResponse;
import com.thali.booking.Booking;
import com.thali.booking.BookingRepository;
import com.thali.booking.BookingStatus;
import com.thali.common.exception.ApiException;
import com.thali.config.AppProperties;
import com.thali.menu.DailyMenu;
import com.thali.menu.DailyMenuRepository;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealSlotService;
import com.thali.menu.MealType;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.UserRepository;

@Service
public class AttendanceService {

    private final BookingRepository bookings;
    private final AttendanceRepository attendance;
    private final DailyMenuRepository menus;
    private final StudentProfileRepository profiles;
    private final UserRepository users;
    private final MealSlotService slots;
    private final AppProperties.Attendance cfg;
    private final Clock clock;

    public AttendanceService(BookingRepository bookings, AttendanceRepository attendance, DailyMenuRepository menus,
                             StudentProfileRepository profiles, UserRepository users, MealSlotService slots,
                             AppProperties props, Clock clock) {
        this.bookings = bookings;
        this.attendance = attendance;
        this.menus = menus;
        this.profiles = profiles;
        this.users = users;
        this.slots = slots;
        this.cfg = props.attendance();
        this.clock = clock;
    }

    @Transactional
    public AttendanceResponse scan(Long staffId, UUID qrToken) {
        Booking b = bookings.findByQr(qrToken)
                .orElseThrow(() -> ApiException.notFound("QR_NOT_RECOGNISED", "This pass isn't valid."));
        return mark(b, staffId, AttendanceMethod.QR);
    }

    @Transactional
    public AttendanceResponse manual(Long staffId, Long bookingId) {
        Booking b = bookings.findDetailed(bookingId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Booking not found."));
        return mark(b, staffId, AttendanceMethod.MANUAL);
    }

    @Transactional(readOnly = true)
    public List<LiveCountsResponse> live(LocalDate date, MealType only) {
        return java.util.Arrays.stream(MealType.values()).filter(t -> only == null || t == only).map(t -> countsFor(date, t)).toList();
    }

    // ------------------------------------------------------------------ internals

    private AttendanceResponse mark(Booking b, Long staffId, AttendanceMethod method) {
        switch (b.getStatus()) {
            case CANCELLED -> throw ApiException.unprocessable("BOOKING_CANCELLED", "This booking was cancelled.");
            case NO_SHOW -> throw ApiException.unprocessable("BOOKING_EXPIRED", "This booking was marked as a no-show.");
            case CONSUMED -> throw ApiException.conflict("ALREADY_SERVED", "This pass has already been used.");
            default -> { }
        }
        DailyMenu menu = b.getDailyMenu();
        MealSlotConfig slot = slots.get(menu.getMealType());
        if (cfg.enforceWindow()) {
            Instant now = clock.instant();
            Instant opens = slots.startsAt(slot, menu.getMenuDate()).minusSeconds(cfg.earlyMinutes() * 60L);
            Instant closes = slots.endsAt(slot, menu.getMenuDate()).plusSeconds(cfg.lateMinutes() * 60L);
            if (now.isBefore(opens) || now.isAfter(closes)) {
                throw ApiException.unprocessable("OUTSIDE_SERVING_WINDOW", "This meal isn't being served right now.");
            }
        }

        b.setStatus(BookingStatus.CONSUMED);
        Attendance a = new Attendance();
        a.setBooking(b);
        a.setMarkedBy(users.getReferenceById(staffId));
        a.setMethod(method);
        a.setMarkedAt(clock.instant());
        attendance.save(a);

        StudentProfile p = profiles.findById(b.getStudent().getId()).orElse(null);
        return new AttendanceResponse(b.getId(), b.getStudent().getName(), p == null ? null : p.getRollNo(),
                p == null ? null : p.getDietPref(), menu.getMenuDate(), menu.getMealType(), method, a.getMarkedAt());
    }

    private LiveCountsResponse countsFor(LocalDate date, MealType type) {
        DailyMenu menu = menus.findByDateAndType(date, type).orElse(null);
        if (menu == null) return new LiveCountsResponse(date, type, null, 0, 0, 0, 0, 0);
        Map<BookingStatus, Long> by = new EnumMap<>(BookingStatus.class);
        for (Object[] row : bookings.countByStatus(menu.getId())) by.put((BookingStatus) row[0], (Long) row[1]);
        long served = by.getOrDefault(BookingStatus.CONSUMED, 0L);
        long remaining = by.getOrDefault(BookingStatus.BOOKED, 0L);
        long noShow = by.getOrDefault(BookingStatus.NO_SHOW, 0L);
        return new LiveCountsResponse(date, type, menu.getId(), served + remaining + noShow, served, remaining, noShow,
                by.getOrDefault(BookingStatus.CANCELLED, 0L));
    }
}
