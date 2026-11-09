package com.thali.booking;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thali.TestProps;
import com.thali.menu.MealSlotConfig;
import com.thali.menu.MealSlotConfigRepository;
import com.thali.menu.MealSlotService;
import com.thali.menu.MealType;
import com.thali.support.Fixtures;
import com.thali.support.MutableClock;
import com.thali.user.Role;

/** Lunch ends 14:30; with the 30 minute grace a still-BOOKED lunch becomes NO_SHOW after 15:00. */
class NoShowServiceTest {

    static final LocalDate TODAY = LocalDate.of(2026, 11, 9);

    MutableClock clock = new MutableClock();
    BookingRepository bookings = mock(BookingRepository.class);
    MealSlotConfigRepository slotRepo = mock(MealSlotConfigRepository.class);
    NoShowService service;

    @BeforeEach
    void setUp() {
        when(slotRepo.findAll()).thenReturn(Fixtures.defaultSlots());
        for (MealSlotConfig s : Fixtures.defaultSlots()) when(slotRepo.findById(s.getMealType())).thenReturn(Optional.of(s));
        service = new NoShowService(bookings, new MealSlotService(slotRepo, clock), TestProps.create(), clock);
    }

    private Booking bookedMeal(LocalDate date, MealType type) {
        return Fixtures.booking(1L, Fixtures.user(7L, Role.STUDENT, "Asha Rao"), Fixtures.menu(1, date, type, true), BookingStatus.BOOKED, "60.00");
    }

    @Test
    void mealsThatEndedMoreThanTheGraceAgoBecomeNoShow() {
        Booking old = bookedMeal(TODAY.minusDays(2), MealType.DINNER);
        Booking lunch = bookedMeal(TODAY, MealType.LUNCH);
        when(bookings.findOpenUpTo(any())).thenReturn(List.of(old, lunch));
        clock.setLocal("2026-11-09T15:01:00");

        assertThat(service.markNoShows()).isEqualTo(2);
        assertThat(old.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
        assertThat(lunch.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
    }

    @Test
    void mealsStillInsideTheServingWindowOrGraceAreLeftAlone() {
        Booking lunch = bookedMeal(TODAY, MealType.LUNCH);
        Booking dinner = bookedMeal(TODAY, MealType.DINNER);
        when(bookings.findOpenUpTo(any())).thenReturn(List.of(lunch, dinner));
        clock.setLocal("2026-11-09T14:45:00");   // lunch is over but inside the 30 min grace

        assertThat(service.markNoShows()).isZero();
        assertThat(lunch.getStatus()).isEqualTo(BookingStatus.BOOKED);
        assertThat(dinner.getStatus()).isEqualTo(BookingStatus.BOOKED);
    }
}
