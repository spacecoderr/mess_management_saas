package com.thali.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.ResultActions;

import com.thali.booking.Booking;
import com.thali.booking.BookingStatus;
import com.thali.menu.DailyMenu;
import com.thali.menu.MealType;
import com.thali.user.User;

class BookingFlowIT extends AbstractIntegrationTest {

    User admin, staff, asha, ben;

    @BeforeEach
    void people() {
        admin = admin();
        staff = staff();
        asha = student("B423001");
        ben = student("B423002");
    }

    private ResultActions book(User who, java.time.LocalDate date, MealType type) throws Exception {
        return post("/api/v1/bookings", who, "{\"date\":\"%s\",\"mealType\":\"%s\"}".formatted(date, type));
    }

    @Test
    void studentsOnlySeePublishedMenusButStaffSeeDrafts() throws Exception {
        menu(TOMORROW, MealType.LUNCH, true, "Dal Tadka");
        menu(TOMORROW, MealType.DINNER, false, "Paneer Masala");
        String url = "/api/v1/menus?from=%s&to=%s".formatted(TODAY, TOMORROW);
        get(url, asha).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].mealType").value("LUNCH"));
        get(url, staff).andExpect(jsonPath("$", hasSize(2))).andExpect(jsonPath("$[0].mealType").value("LUNCH")).andExpect(jsonPath("$[1].mealType").value("DINNER"));
    }

    @Test
    void aDayInTheLife_publishBookScanRate() throws Exception {
        // staff prepares tonight's dinner through the API
        long dishId = readId(post("/api/v1/menu-items", admin, "{\"name\":\"Dal Tadka\",\"category\":\"MAIN\",\"veg\":true}").andExpect(status().isCreated()));
        long menuId = readId(put("/api/v1/menus/%s/DINNER".formatted(TODAY), staff, "{\"itemIds\":[%d],\"publish\":true}".formatted(dishId))
                .andExpect(status().isOk()).andExpect(jsonPath("$.published").value(true)).andExpect(jsonPath("$.items[0].name").value("Dal Tadka"))
                .andExpect(jsonPath("$.bookingOpen").value(true)));   // dinner 20:00 - 4h cutoff = 16:00, now is 10:30

        // the student books and gets a QR pass
        ResultActions booked = book(asha, TODAY, MealType.DINNER).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("BOOKED")).andExpect(jsonPath("$.price").value(60.0));
        long bookingId = readId(booked);
        String token = read(booked, "$.qrToken");
        get("/api/v1/bookings/%d/qr".formatted(bookingId), asha).andExpect(status().isOk()).andExpect(jsonPath("$.payload").value("THALI:" + token));

        // rating before eating is refused
        post("/api/v1/feedback", asha, "{\"dailyMenuId\":%d,\"rating\":5}".formatted(menuId)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("NOT_EATEN"));

        // at the counter during dinner: scan works once, not twice
        clock.setLocal("2026-11-09T20:05:00");
        post("/api/v1/attendance/scan", staff, "{\"qrToken\":\"%s\"}".formatted(token)).andExpect(status().isOk()).andExpect(jsonPath("$.rollNo").value("B423001"));
        post("/api/v1/attendance/scan", staff, "{\"qrToken\":\"%s\"}".formatted(token)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_SERVED"));
        get("/api/v1/attendance/live?mealType=DINNER", staff).andExpect(jsonPath("$[0].served").value(1)).andExpect(jsonPath("$[0].remaining").value(0));

        // now the student can rate - once
        get("/api/v1/feedback/me/pending", asha).andExpect(jsonPath("$", hasSize(1)));
        post("/api/v1/feedback", asha, "{\"dailyMenuId\":%d,\"rating\":4,\"comment\":\"Tasty\"}".formatted(menuId)).andExpect(status().isCreated());
        post("/api/v1/feedback", asha, "{\"dailyMenuId\":%d,\"rating\":2}".formatted(menuId)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_RATED"));
        get("/api/v1/feedback/me/pending", asha).andExpect(jsonPath("$", hasSize(0)));
        get("/api/v1/feedback/summary", staff).andExpect(jsonPath("$.averageRating").value(4.0)).andExpect(jsonPath("$.totalRatings").value(1));

        // a student who never booked can't rate it either
        post("/api/v1/feedback", ben, "{\"dailyMenuId\":%d,\"rating\":1}".formatted(menuId)).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cutoffIsEnforcedExactlyAtMealStartMinusCutoff() throws Exception {
        menu(TODAY, MealType.LUNCH, true, "Dal");                        // lunch 12:30, cutoff 4h -> closes 08:30
        book(asha, TODAY, MealType.LUNCH).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("BOOKING_CLOSED"));

        clock.setLocal("2026-11-09T08:29:59");
        long id = readId(book(asha, TODAY, MealType.LUNCH).andExpect(status().isCreated()));

        clock.setLocal("2026-11-09T08:30:00");
        delete("/api/v1/bookings/" + id, asha).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("CANCEL_CLOSED"));
        book(ben, TODAY, MealType.LUNCH).andExpect(status().isUnprocessableEntity());
    }

    @Test
    void cancelledBookingsCanBeRebookedWithAFreshQr() throws Exception {
        menu(TOMORROW, MealType.LUNCH, true, "Dal");
        ResultActions first = book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isCreated());
        long id = readId(first);
        String oldToken = read(first, "$.qrToken");

        book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("ALREADY_BOOKED"));
        delete("/api/v1/bookings/" + id, asha).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("CANCELLED"));
        get("/api/v1/bookings/%d/qr".formatted(id), asha).andExpect(status().isUnprocessableEntity());

        ResultActions again = book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("BOOKED"));
        assertThat(readId(again)).isEqualTo(id);
        assertThat((String) read(again, "$.qrToken")).isNotEqualTo(oldToken);
    }

    @Test
    void draftOrMissingMenusCannotBeBooked() throws Exception {
        menu(TOMORROW, MealType.LUNCH, false, "Dal");
        book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("MENU_NOT_PUBLISHED"));
        book(asha, TOMORROW, MealType.DINNER).andExpect(status().isNotFound()).andExpect(jsonPath("$.code").value("MENU_NOT_FOUND"));
    }

    @Test
    void aMenuWithLiveBookingsCannotBeUnpublished() throws Exception {
        DailyMenu m = menu(TOMORROW, MealType.LUNCH, true, "Dal");
        long bookingId = readId(book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isCreated()));
        patch("/api/v1/menus/%d/publish".formatted(m.getId()), staff, "{\"published\":false}").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("MENU_HAS_BOOKINGS"));
        delete("/api/v1/bookings/" + bookingId, asha).andExpect(status().isOk());
        patch("/api/v1/menus/%d/publish".formatted(m.getId()), staff, "{\"published\":false}").andExpect(status().isOk()).andExpect(jsonPath("$.published").value(false));
    }

    @Test
    void studentsCannotSeeOrCancelEachOthersBookings() throws Exception {
        menu(TOMORROW, MealType.LUNCH, true, "Dal");
        long id = readId(book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isCreated()));
        delete("/api/v1/bookings/" + id, ben).andExpect(status().isNotFound());
        get("/api/v1/bookings/%d/qr".formatted(id), ben).andExpect(status().isNotFound());
        get("/api/v1/bookings/me?from=%s&to=%s".formatted(TODAY, TOMORROW), ben).andExpect(jsonPath("$", hasSize(0)));
        get("/api/v1/bookings/me?from=%s&to=%s".formatted(TODAY, TOMORROW), asha).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void staffListsHideQrTokensAndSupportSearchByRollNumber() throws Exception {
        menu(TOMORROW, MealType.LUNCH, true, "Dal");
        book(asha, TOMORROW, MealType.LUNCH);
        book(ben, TOMORROW, MealType.LUNCH);
        get("/api/v1/bookings?date=%s&mealType=LUNCH&q=b423002".formatted(TOMORROW), staff)
                .andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].rollNo").value("B423002")).andExpect(jsonPath("$[0].qrToken").doesNotExist());
        get("/api/v1/bookings?date=%s".formatted(TOMORROW), staff).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void bulkBookingKeepsWhatWorkedAndExplainsTheRest() throws Exception {
        menu(TOMORROW, MealType.LUNCH, true, "Dal");
        menu(TODAY, MealType.LUNCH, true, "Dal");   // already closed
        post("/api/v1/bookings/bulk", asha, """
                {"items":[{"date":"%s","mealType":"LUNCH"},{"date":"%s","mealType":"LUNCH"},{"date":"%s","mealType":"DINNER"}]}"""
                .formatted(TOMORROW, TODAY, TOMORROW)).andExpect(status().isOk())
                .andExpect(jsonPath("$.booked", hasSize(1))).andExpect(jsonPath("$.skipped", hasSize(2)))
                .andExpect(jsonPath("$.skipped[0].code").value("BOOKING_CLOSED")).andExpect(jsonPath("$.skipped[1].code").value("MENU_NOT_FOUND"));
    }

    @Test
    void scansOutsideTheServingWindowAreRefusedAndInsideAreAccepted() throws Exception {
        DailyMenu m = menu(TOMORROW, MealType.LUNCH, true, "Dal");
        Booking b = booking(asha, m, BookingStatus.BOOKED);
        String body = "{\"qrToken\":\"%s\"}".formatted(b.getQrToken());

        post("/api/v1/attendance/scan", staff, body).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("OUTSIDE_SERVING_WINDOW"));
        clock.setLocal("2026-11-10T11:59:00");
        post("/api/v1/attendance/scan", staff, body).andExpect(status().isUnprocessableEntity());
        clock.setLocal("2026-11-10T12:00:00");   // 30 min before lunch starts
        post("/api/v1/attendance/scan", staff, body).andExpect(status().isOk());
        assertThat(bookings.findById(b.getId()).orElseThrow().getStatus()).isEqualTo(BookingStatus.CONSUMED);
    }

    @Test
    void cancelledAndUnknownPassesAreNotServed() throws Exception {
        DailyMenu m = menu(TOMORROW, MealType.LUNCH, true, "Dal");
        Booking cancelled = booking(asha, m, BookingStatus.CANCELLED);
        clock.setLocal("2026-11-10T12:45:00");
        post("/api/v1/attendance/scan", staff, "{\"qrToken\":\"%s\"}".formatted(cancelled.getQrToken())).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("BOOKING_CANCELLED"));
        post("/api/v1/attendance/scan", staff, "{\"qrToken\":\"11111111-1111-1111-1111-111111111111\"}").andExpect(status().isNotFound());
        post("/api/v1/attendance/scan", staff, "{\"qrToken\":\"nope\"}").andExpect(status().isBadRequest());
    }

    @Test
    void manualMarkingByStaffWorksAndCannotBeRepeated() throws Exception {
        DailyMenu m = menu(TOMORROW, MealType.LUNCH, true, "Dal");
        Booking b = booking(asha, m, BookingStatus.BOOKED);
        clock.setLocal("2026-11-10T13:00:00");
        post("/api/v1/attendance/manual", staff, "{\"bookingId\":%d}".formatted(b.getId())).andExpect(status().isOk()).andExpect(jsonPath("$.method").value("MANUAL"));
        post("/api/v1/attendance/manual", staff, "{\"bookingId\":%d}".formatted(b.getId())).andExpect(status().isConflict());
    }

    @Test
    void priceIsFrozenWhenBookedEvenIfTheAdminChangesItLater() throws Exception {
        menu(TOMORROW, MealType.LUNCH, true, "Dal");
        long id = readId(book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isCreated()).andExpect(jsonPath("$.price").value(60.0)));
        put("/api/v1/meal-slots/LUNCH", admin, "{\"startTime\":\"12:30\",\"endTime\":\"14:30\",\"cutoffMinutes\":240,\"price\":80,\"active\":true}").andExpect(status().isOk());
        assertThat(bookings.findById(id).orElseThrow().getPriceAtBooking()).isEqualByComparingTo("60.00");
        book(ben, TOMORROW, MealType.LUNCH).andExpect(jsonPath("$.price").value(80.0));
    }

    @Test
    void invalidSlotTimesAndInactiveMealsAreRejected() throws Exception {
        put("/api/v1/meal-slots/LUNCH", admin, "{\"startTime\":\"14:30\",\"endTime\":\"12:30\",\"cutoffMinutes\":240,\"price\":60,\"active\":true}")
                .andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("INVALID_TIME_RANGE"));
        menu(TOMORROW, MealType.LUNCH, true, "Dal");
        put("/api/v1/meal-slots/LUNCH", admin, "{\"startTime\":\"12:30\",\"endTime\":\"14:30\",\"cutoffMinutes\":240,\"price\":60,\"active\":false}").andExpect(status().isOk());
        book(asha, TOMORROW, MealType.LUNCH).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("MEAL_INACTIVE"));
    }

    @Test
    void menuEditingRules() throws Exception {
        long dal = dish("Dal").getId();
        put("/api/v1/menus/2026-11-08/LUNCH", staff, "{\"itemIds\":[%d]}".formatted(dal)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("MENU_IN_PAST"));
        put("/api/v1/menus/%s/LUNCH".formatted(TOMORROW), staff, "{\"itemIds\":[99999]}").andExpect(status().isBadRequest()).andExpect(jsonPath("$.code").value("DISH_NOT_FOUND"));
        put("/api/v1/menus/%s/LUNCH".formatted(TOMORROW), staff, "{\"itemIds\":[],\"publish\":true}").andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("MENU_EMPTY"));
        put("/api/v1/menus/%s/LUNCH".formatted(TOMORROW), staff, "{\"itemIds\":[%d],\"publish\":true}".formatted(dal)).andExpect(status().isOk());
        delete("/api/v1/menu-items/" + dal, admin).andExpect(status().isNoContent());
        put("/api/v1/menus/%s/DINNER".formatted(TOMORROW), staff, "{\"itemIds\":[%d]}".formatted(dal)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("DISH_INACTIVE"));
    }

    @Test
    void copyWeekClonesDraftsAndDoesNotOverwriteByDefault() throws Exception {
        menu(TODAY, MealType.LUNCH, true, "Dal");
        menu(TODAY.plusDays(1), MealType.DINNER, true, "Paneer");
        String body = "{\"fromWeekStart\":\"%s\",\"toWeekStart\":\"%s\"}".formatted(TODAY, TODAY.plusDays(7));
        post("/api/v1/menus/copy-week", staff, body).andExpect(jsonPath("$.copied").value(2)).andExpect(jsonPath("$.skipped").value(0));
        post("/api/v1/menus/copy-week", staff, body).andExpect(jsonPath("$.copied").value(0)).andExpect(jsonPath("$.skipped").value(2));
        get("/api/v1/menus?from=%s&to=%s".formatted(TODAY.plusDays(7), TODAY.plusDays(8)), asha).andExpect(jsonPath("$", hasSize(0)));   // copies are drafts
        get("/api/v1/menus?from=%s&to=%s".formatted(TODAY.plusDays(7), TODAY.plusDays(8)), staff).andExpect(jsonPath("$", hasSize(2)));
    }

    @Test
    void duplicateDishNamesAreRejectedCaseInsensitively() throws Exception {
        post("/api/v1/menu-items", admin, "{\"name\":\"Idli\",\"category\":\"MAIN\",\"veg\":true}").andExpect(status().isCreated());
        post("/api/v1/menu-items", admin, "{\"name\":\"idli\",\"category\":\"MAIN\",\"veg\":true}").andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("DISH_EXISTS"));
    }
}
