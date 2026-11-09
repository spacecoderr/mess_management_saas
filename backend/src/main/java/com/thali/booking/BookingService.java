package com.thali.booking;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.booking.dto.BookingRequest;
import com.thali.booking.dto.BookingResponse;
import com.thali.booking.dto.BulkBookingResponse;
import com.thali.booking.dto.QrResponse;
import com.thali.common.exception.ApiException;
import com.thali.menu.DailyMenu;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealSlotService;
import com.thali.menu.MealType;
import com.thali.menu.MenuItem;
import com.thali.menu.MenuService;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.UserRepository;

/** Booking rules: menu must be published, meal active, and the cutoff (start - cutoffMinutes) not yet passed. */
@Service
public class BookingService {

    private final BookingRepository bookings;
    private final MenuService menus;
    private final MealSlotService slots;
    private final UserRepository users;
    private final StudentProfileRepository profiles;
    private final Clock clock;

    public BookingService(BookingRepository bookings, MenuService menus, MealSlotService slots, UserRepository users,
                          StudentProfileRepository profiles, Clock clock) {
        this.bookings = bookings;
        this.menus = menus;
        this.slots = slots;
        this.users = users;
        this.profiles = profiles;
        this.clock = clock;
    }

    @Transactional
    public BookingResponse book(Long studentId, BookingRequest req) {
        return toStudentView(create(studentId, req));
    }

    /** Best-effort: each item is validated up-front, so one failure never blocks the rest. */
    @Transactional
    public BulkBookingResponse bookMany(Long studentId, List<BookingRequest> items) {
        List<BookingResponse> booked = new ArrayList<>();
        List<BulkBookingResponse.Skipped> skipped = new ArrayList<>();
        for (BookingRequest item : items) {
            try {
                booked.add(toStudentView(create(studentId, item)));
            } catch (ApiException e) {
                skipped.add(new BulkBookingResponse.Skipped(item.date(), item.mealType(), e.getCode(), e.getMessage()));
            }
        }
        return new BulkBookingResponse(booked, skipped);
    }

    @Transactional
    public BookingResponse cancel(Long studentId, Long bookingId) {
        Booking b = owned(studentId, bookingId);
        if (b.getStatus() != BookingStatus.BOOKED) {
            throw ApiException.unprocessable("NOT_CANCELLABLE", "Only active bookings can be cancelled.");
        }
        DailyMenu menu = b.getDailyMenu();
        if (!clock.instant().isBefore(closesAt(menu))) {
            throw ApiException.unprocessable("CANCEL_CLOSED", "The cutoff for this meal has passed, so it can't be cancelled.");
        }
        b.setStatus(BookingStatus.CANCELLED);
        b.setCancelledAt(clock.instant());
        return toStudentView(b);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> mine(Long studentId, LocalDate from, LocalDate to) {
        if (to.isBefore(from)) throw ApiException.badRequest("INVALID_RANGE", "`to` must not be before `from`.");
        return bookings.findForStudent(studentId, from, to).stream()
                .sorted(java.util.Comparator.comparing((Booking b) -> b.getDailyMenu().getMenuDate()).thenComparing(b -> b.getDailyMenu().getMealType()))
                .map(this::toStudentView).toList();
    }

    @Transactional(readOnly = true)
    public QrResponse qr(Long studentId, Long bookingId) {
        Booking b = owned(studentId, bookingId);
        if (b.getStatus() == BookingStatus.CANCELLED) {
            throw ApiException.unprocessable("BOOKING_CANCELLED", "This booking was cancelled.");
        }
        DailyMenu m = b.getDailyMenu();
        return new QrResponse(b.getId(), b.getQrToken(), "THALI:" + b.getQrToken(), b.getStatus(), m.getMenuDate(), m.getMealType());
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> forStaff(LocalDate date, MealType type, BookingStatus status, String q) {
        Collection<MealType> types = type == null ? EnumSet.allOf(MealType.class) : EnumSet.of(type);
        Collection<BookingStatus> statuses = status == null ? EnumSet.allOf(BookingStatus.class) : EnumSet.of(status);
        String like = q == null || q.isBlank() ? "%" : "%" + q.trim().toLowerCase(Locale.ROOT) + "%";
        List<Booking> list = bookings.findForStaff(date, types, statuses, like);
        Map<Long, StudentProfile> profileById = profiles.findAllById(list.stream().map(b -> b.getStudent().getId()).toList())
                .stream().collect(Collectors.toMap(StudentProfile::getUserId, p -> p));
        return list.stream().map(b -> toStaffView(b, profileById.get(b.getStudent().getId()))).toList();
    }

    // ------------------------------------------------------------------ internals

    private Booking create(Long studentId, BookingRequest req) {
        DailyMenu menu = menus.require(req.date(), req.mealType());
        if (!menu.isPublished()) {
            throw ApiException.unprocessable("MENU_NOT_PUBLISHED", "The menu for this meal isn't published yet.");
        }
        MealSlotConfig slot = slots.get(req.mealType());
        if (!slot.isActive()) {
            throw ApiException.unprocessable("MEAL_INACTIVE", "This meal isn't being served.");
        }
        if (!clock.instant().isBefore(slots.bookingClosesAt(slot, req.date()))) {
            throw ApiException.unprocessable("BOOKING_CLOSED", "Booking for this meal has closed.");
        }

        Booking existing = bookings.findByStudentIdAndDailyMenuId(studentId, menu.getId()).orElse(null);
        if (existing != null && existing.getStatus() != BookingStatus.CANCELLED) {
            throw ApiException.conflict("ALREADY_BOOKED", "You've already booked this meal.");
        }
        Booking b = existing != null ? existing : new Booking();
        if (existing == null) {
            b.setStudent(users.getReferenceById(studentId));
            b.setDailyMenu(menu);
        }
        b.setStatus(BookingStatus.BOOKED);
        b.setCancelledAt(null);
        b.setPriceAtBooking(slot.getPrice());     // price is frozen at booking time
        b.setQrToken(UUID.randomUUID());          // a re-booking after cancel gets a fresh pass
        return bookings.save(b);
    }

    private Booking owned(Long studentId, Long bookingId) {
        Booking b = bookings.findDetailed(bookingId)
                .orElseThrow(() -> ApiException.notFound("BOOKING_NOT_FOUND", "Booking not found."));
        if (!b.getStudent().getId().equals(studentId)) {   // 404, not 403: don't reveal other students' ids
            throw ApiException.notFound("BOOKING_NOT_FOUND", "Booking not found.");
        }
        return b;
    }

    private Instant closesAt(DailyMenu menu) {
        return slots.bookingClosesAt(slots.get(menu.getMealType()), menu.getMenuDate());
    }

    private BookingResponse toStudentView(Booking b) {
        return build(b, true, null);
    }

    private BookingResponse toStaffView(Booking b, StudentProfile p) {
        return build(b, false, p);
    }

    private BookingResponse build(Booking b, boolean ownerView, StudentProfile profile) {
        DailyMenu m = b.getDailyMenu();
        MealSlotConfig slot = slots.get(m.getMealType());
        Instant closes = slots.bookingClosesAt(slot, m.getMenuDate());
        boolean cancellable = b.getStatus() == BookingStatus.BOOKED && clock.instant().isBefore(closes);
        return new BookingResponse(b.getId(), m.getId(), m.getMenuDate(), m.getMealType(), b.getStatus(), b.getPriceAtBooking(),
                ownerView ? b.getQrToken() : null, slot.getStartTime(), slot.getEndTime(), closes, cancellable,
                m.getItems().stream().map(MenuItem::getName).sorted().toList(),
                ownerView ? null : b.getStudent().getId(), ownerView ? null : b.getStudent().getName(),
                profile == null ? null : profile.getRollNo());
    }
}
