package com.thali.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thali.booking.dto.BookingRequest;
import com.thali.booking.dto.BookingResponse;
import com.thali.booking.dto.BulkBookingResponse;
import com.thali.common.exception.ApiException;
import com.thali.menu.DailyMenu;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealSlotConfigRepository;
import com.thali.menu.MealSlotService;
import com.thali.menu.MealType;
import com.thali.menu.MenuService;
import com.thali.support.Fixtures;
import com.thali.support.MutableClock;
import com.thali.user.Role;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;
import com.thali.user.UserRepository;

/** Clock is fixed at Mon 9 Nov 2026 10:30 IST. Lunch (12:30, 4h cutoff) closed at 08:30; dinner (20:00) closes at 16:00. */
class BookingServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 9), TOMORROW = TODAY.plusDays(1);

    MutableClock clock = new MutableClock();
    BookingRepository bookings = mock(BookingRepository.class);
    MenuService menus = mock(MenuService.class);
    MealSlotConfigRepository slotRepo = mock(MealSlotConfigRepository.class);
    UserRepository users = mock(UserRepository.class);
    StudentProfileRepository profiles = mock(StudentProfileRepository.class);
    User student = Fixtures.user(7L, Role.STUDENT, "Asha Rao");
    BookingService service;

    @BeforeEach
    void setUp() {
        for (MealSlotConfig s : Fixtures.defaultSlots()) when(slotRepo.findById(s.getMealType())).thenReturn(Optional.of(s));
        service = new BookingService(bookings, menus, new MealSlotService(slotRepo, clock), users, profiles, clock);
        when(bookings.save(any(Booking.class))).thenAnswer(i -> i.getArgument(0));
        when(users.getReferenceById(7L)).thenReturn(student);
    }

    private DailyMenu publishedMenu(long id, LocalDate date, MealType type) {
        DailyMenu m = Fixtures.menu(id, date, type, true);
        when(menus.require(date, type)).thenReturn(m);
        return m;
    }

    private static void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", code);
    }

    @Test
    void booksAnOpenMealAtTheCurrentSlotPrice() {
        publishedMenu(1, TOMORROW, MealType.LUNCH);
        BookingResponse r = service.book(7L, new BookingRequest(TOMORROW, MealType.LUNCH));
        assertThat(r.status()).isEqualTo(BookingStatus.BOOKED);
        assertThat(r.price()).isEqualByComparingTo("60.00");
        assertThat(r.qrToken()).isNotNull();
        assertThat(r.cancellable()).isTrue();
    }

    @Test
    void bookingClosesExactlyAtMealStartMinusCutoff() {
        publishedMenu(2, TODAY, MealType.DINNER);                    // 20:00 - 4h = 16:00
        clock.setLocal("2026-11-09T15:59:59");
        assertThat(service.book(7L, new BookingRequest(TODAY, MealType.DINNER)).status()).isEqualTo(BookingStatus.BOOKED);

        clock.setLocal("2026-11-09T16:00:00");
        assertCode(() -> service.book(7L, new BookingRequest(TODAY, MealType.DINNER)), "BOOKING_CLOSED");
    }

    @Test
    void cannotBookAMealWhoseCutoffAlreadyPassed() {
        publishedMenu(3, TODAY, MealType.LUNCH);                     // closed at 08:30, it is 10:30
        assertCode(() -> service.book(7L, new BookingRequest(TODAY, MealType.LUNCH)), "BOOKING_CLOSED");
    }

    @Test
    void snacksUseTheirOwnShorterCutoff() {
        publishedMenu(4, TODAY, MealType.SNACKS);                    // 17:00 - 2h = 15:00
        assertThat(service.book(7L, new BookingRequest(TODAY, MealType.SNACKS)).status()).isEqualTo(BookingStatus.BOOKED);
        clock.setLocal("2026-11-09T15:00:00");
        assertCode(() -> service.book(7L, new BookingRequest(TODAY, MealType.SNACKS)), "BOOKING_CLOSED");
    }

    @Test
    void cannotBookAnUnpublishedMenu() {
        when(menus.require(TOMORROW, MealType.LUNCH)).thenReturn(Fixtures.menu(5, TOMORROW, MealType.LUNCH, false));
        assertCode(() -> service.book(7L, new BookingRequest(TOMORROW, MealType.LUNCH)), "MENU_NOT_PUBLISHED");
    }

    @Test
    void cannotBookAnInactiveMeal() {
        publishedMenu(6, TOMORROW, MealType.LUNCH);
        slotRepo.findById(MealType.LUNCH).get().setActive(false);
        assertCode(() -> service.book(7L, new BookingRequest(TOMORROW, MealType.LUNCH)), "MEAL_INACTIVE");
    }

    @Test
    void doubleBookingIsRejected() {
        DailyMenu m = publishedMenu(7, TOMORROW, MealType.LUNCH);
        when(bookings.findByStudentIdAndDailyMenuId(7L, 7L)).thenReturn(Optional.of(Fixtures.booking(1L, student, m, BookingStatus.BOOKED, "60.00")));
        assertCode(() -> service.book(7L, new BookingRequest(TOMORROW, MealType.LUNCH)), "ALREADY_BOOKED");
    }

    @Test
    void rebookingACancelledMealReusesTheRowWithAFreshQrAndCurrentPrice() {
        DailyMenu m = publishedMenu(8, TOMORROW, MealType.LUNCH);
        Booking old = Fixtures.booking(1L, student, m, BookingStatus.CANCELLED, "50.00");
        var oldToken = old.getQrToken();
        when(bookings.findByStudentIdAndDailyMenuId(7L, 8L)).thenReturn(Optional.of(old));

        BookingResponse r = service.book(7L, new BookingRequest(TOMORROW, MealType.LUNCH));

        assertThat(r.id()).isEqualTo(1L);
        assertThat(r.status()).isEqualTo(BookingStatus.BOOKED);
        assertThat(r.price()).isEqualByComparingTo("60.00");
        assertThat(r.qrToken()).isNotEqualTo(oldToken);
        assertThat(old.getCancelledAt()).isNull();
    }

    @Test
    void cancelWorksBeforeTheCutoffAndFreesTheBooking() {
        DailyMenu m = Fixtures.menu(9, TOMORROW, MealType.LUNCH, true);
        Booking b = Fixtures.booking(1L, student, m, BookingStatus.BOOKED, "60.00");
        when(bookings.findDetailed(1L)).thenReturn(Optional.of(b));
        BookingResponse r = service.cancel(7L, 1L);
        assertThat(r.status()).isEqualTo(BookingStatus.CANCELLED);
        assertThat(b.getCancelledAt()).isEqualTo(clock.instant());
    }

    @Test
    void cancelAfterTheCutoffIsRefused() {
        DailyMenu m = Fixtures.menu(10, TODAY, MealType.LUNCH, true);   // lunch closed at 08:30
        when(bookings.findDetailed(1L)).thenReturn(Optional.of(Fixtures.booking(1L, student, m, BookingStatus.BOOKED, "60.00")));
        assertCode(() -> service.cancel(7L, 1L), "CANCEL_CLOSED");
    }

    @Test
    void servedOrAlreadyCancelledBookingsCannotBeCancelled() {
        DailyMenu m = Fixtures.menu(11, TOMORROW, MealType.LUNCH, true);
        for (BookingStatus s : List.of(BookingStatus.CONSUMED, BookingStatus.CANCELLED, BookingStatus.NO_SHOW)) {
            when(bookings.findDetailed(1L)).thenReturn(Optional.of(Fixtures.booking(1L, student, m, s, "60.00")));
            assertCode(() -> service.cancel(7L, 1L), "NOT_CANCELLABLE");
        }
    }

    @Test
    void aStudentCannotCancelSomeoneElsesBooking() {
        DailyMenu m = Fixtures.menu(12, TOMORROW, MealType.LUNCH, true);
        User other = Fixtures.user(99L, Role.STUDENT, "Other Person");
        when(bookings.findDetailed(1L)).thenReturn(Optional.of(Fixtures.booking(1L, other, m, BookingStatus.BOOKED, "60.00")));
        assertCode(() -> service.cancel(7L, 1L), "BOOKING_NOT_FOUND");   // 404, not 403: no information leak
    }

    @Test
    void bulkBookingKeepsWhatWorkedAndReportsWhatDidNot() {
        publishedMenu(13, TOMORROW, MealType.LUNCH);
        publishedMenu(14, TODAY, MealType.LUNCH);                    // already closed
        BulkBookingResponse r = service.bookMany(7L, List.of(new BookingRequest(TOMORROW, MealType.LUNCH), new BookingRequest(TODAY, MealType.LUNCH)));
        assertThat(r.booked()).hasSize(1);
        assertThat(r.skipped()).singleElement().satisfies(s -> {
            assertThat(s.code()).isEqualTo("BOOKING_CLOSED");
            assertThat(s.mealType()).isEqualTo(MealType.LUNCH);
        });
    }

    @Test
    void qrIsNotIssuedForACancelledBooking() {
        DailyMenu m = Fixtures.menu(15, TOMORROW, MealType.LUNCH, true);
        when(bookings.findDetailed(1L)).thenReturn(Optional.of(Fixtures.booking(1L, student, m, BookingStatus.CANCELLED, "60.00")));
        assertCode(() -> service.qr(7L, 1L), "BOOKING_CANCELLED");
    }
}
