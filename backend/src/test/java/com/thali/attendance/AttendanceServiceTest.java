package com.thali.attendance;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thali.TestProps;
import com.thali.attendance.dto.AttendanceResponse;
import com.thali.attendance.dto.LiveCountsResponse;
import com.thali.booking.Booking;
import com.thali.booking.BookingRepository;
import com.thali.booking.BookingStatus;
import com.thali.common.exception.ApiException;
import com.thali.menu.DailyMenu;
import com.thali.menu.DailyMenuRepository;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealSlotConfigRepository;
import com.thali.menu.MealSlotService;
import com.thali.menu.MealType;
import com.thali.support.Fixtures;
import com.thali.support.MutableClock;
import com.thali.user.Role;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;
import com.thali.user.UserRepository;

/** Lunch is served 12:30-14:30, so with the 30-minute grace on each side QR scans are accepted 12:00-15:00. */
class AttendanceServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    MutableClock clock = new MutableClock().setLocal("2026-11-09T13:00:00");
    BookingRepository bookings = mock(BookingRepository.class);
    AttendanceRepository attendance = mock(AttendanceRepository.class);
    DailyMenuRepository menus = mock(DailyMenuRepository.class);
    StudentProfileRepository profiles = mock(StudentProfileRepository.class);
    UserRepository users = mock(UserRepository.class);
    MealSlotConfigRepository slotRepo = mock(MealSlotConfigRepository.class);
    User student = Fixtures.user(7L, Role.STUDENT, "Asha Rao");
    User staff = Fixtures.user(3L, Role.STAFF, "Ramesh Staff");
    DailyMenu lunch = Fixtures.menu(1L, TODAY, MealType.LUNCH, true);

    AttendanceService service(boolean enforceWindow) {
        return new AttendanceService(bookings, attendance, menus, profiles, users, new MealSlotService(slotRepo, clock),
                TestProps.create(enforceWindow, "300.00"), clock);
    }

    @BeforeEach
    void setUp() {
        for (MealSlotConfig s : Fixtures.defaultSlots()) when(slotRepo.findById(s.getMealType())).thenReturn(Optional.of(s));
        when(users.getReferenceById(3L)).thenReturn(staff);
        when(profiles.findById(7L)).thenReturn(Optional.of(Fixtures.profile(student, "B423046")));
    }

    private Booking booked(BookingStatus status) {
        Booking b = Fixtures.booking(1L, student, lunch, status, "60.00");
        when(bookings.findByQr(b.getQrToken())).thenReturn(Optional.of(b));
        return b;
    }

    private static void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", code);
    }

    @Test
    void scanningAValidPassMarksTheMealServedAndRecordsWho() {
        Booking b = booked(BookingStatus.BOOKED);
        AttendanceResponse r = service(true).scan(3L, b.getQrToken());

        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONSUMED);
        assertThat(r.rollNo()).isEqualTo("B423046");
        assertThat(r.method()).isEqualTo(AttendanceMethod.QR);
        verify(attendance).save(any(Attendance.class));
    }

    @Test
    void scanningTheSamePassTwiceIsRejectedAndRecordsNothingNew() {
        Booking b = booked(BookingStatus.BOOKED);
        AttendanceService svc = service(true);
        svc.scan(3L, b.getQrToken());
        assertCode(() -> svc.scan(3L, b.getQrToken()), "ALREADY_SERVED");
        verify(attendance).save(any(Attendance.class));   // exactly once
    }

    @Test
    void cancelledAndNoShowBookingsCannotBeServed() {
        Booking cancelled = booked(BookingStatus.CANCELLED);
        assertCode(() -> service(true).scan(3L, cancelled.getQrToken()), "BOOKING_CANCELLED");
        Booking missed = booked(BookingStatus.NO_SHOW);
        assertCode(() -> service(true).scan(3L, missed.getQrToken()), "BOOKING_EXPIRED");
        verify(attendance, never()).save(any());
    }

    @Test
    void unknownPassIsNotRecognised() {
        when(bookings.findByQr(any())).thenReturn(Optional.empty());
        assertCode(() -> service(true).scan(3L, java.util.UUID.randomUUID()), "QR_NOT_RECOGNISED");
    }

    @Test
    void passesAreOnlyAcceptedInsideTheServingWindowPlusGrace() {
        Booking b = booked(BookingStatus.BOOKED);
        clock.setLocal("2026-11-09T11:59:59");   // 1 second before the grace opens
        assertCode(() -> service(true).scan(3L, b.getQrToken()), "OUTSIDE_SERVING_WINDOW");
        clock.setLocal("2026-11-09T15:00:01");   // 1 second after the grace ends
        assertCode(() -> service(true).scan(3L, b.getQrToken()), "OUTSIDE_SERVING_WINDOW");
        clock.setLocal("2026-11-09T12:00:00");   // grace opens
        service(true).scan(3L, b.getQrToken());
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONSUMED);
    }

    @Test
    void windowCanBeSwitchedOffForLocalDemos() {
        Booking b = booked(BookingStatus.BOOKED);
        clock.setLocal("2026-11-09T03:00:00");
        service(false).scan(3L, b.getQrToken());
        assertThat(b.getStatus()).isEqualTo(BookingStatus.CONSUMED);
    }

    @Test
    void manualMarkingUsesTheSameRules() {
        Booking b = Fixtures.booking(1L, student, lunch, BookingStatus.CONSUMED, "60.00");
        when(bookings.findDetailed(1L)).thenReturn(Optional.of(b));
        assertCode(() -> service(true).manual(3L, 1L), "ALREADY_SERVED");

        Booking fresh = Fixtures.booking(2L, student, lunch, BookingStatus.BOOKED, "60.00");
        when(bookings.findDetailed(2L)).thenReturn(Optional.of(fresh));
        assertThat(service(true).manual(3L, 2L).method()).isEqualTo(AttendanceMethod.MANUAL);
    }

    @Test
    void liveCountsAddUpPerStatus() {
        when(menus.findByDateAndType(TODAY, MealType.LUNCH)).thenReturn(Optional.of(lunch));
        when(bookings.countByStatus(1L)).thenReturn(List.of(new Object[] {BookingStatus.CONSUMED, 12L}, new Object[] {BookingStatus.BOOKED, 5L},
                new Object[] {BookingStatus.NO_SHOW, 2L}, new Object[] {BookingStatus.CANCELLED, 4L}));
        LiveCountsResponse live = service(true).live(TODAY, MealType.LUNCH).get(0);
        assertThat(live.booked()).isEqualTo(19);    // served + remaining + no-show, cancelled excluded
        assertThat(live.served()).isEqualTo(12);
        assertThat(live.remaining()).isEqualTo(5);
        assertThat(live.cancelled()).isEqualTo(4);
    }

    @Test
    void liveCountsAreZeroWhenNoMenuExists() {
        when(menus.findByDateAndType(TODAY, MealType.SNACKS)).thenReturn(Optional.empty());
        LiveCountsResponse live = service(true).live(TODAY, MealType.SNACKS).get(0);
        assertThat(live.menuId()).isNull();
        assertThat(live.booked()).isZero();
    }
}
