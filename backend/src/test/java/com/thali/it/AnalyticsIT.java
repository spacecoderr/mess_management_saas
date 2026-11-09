package com.thali.it;

import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.thali.booking.BookingStatus;
import com.thali.complaint.Complaint;
import com.thali.complaint.ComplaintCategory;
import com.thali.complaint.ComplaintRepository;
import com.thali.complaint.ComplaintStatus;
import com.thali.feedback.Feedback;
import com.thali.feedback.FeedbackRepository;
import com.thali.menu.DailyMenu;
import com.thali.menu.MealType;
import com.thali.user.ApprovalStatus;
import com.thali.user.User;

/** Dataset: three approved students + one pending; today's lunch (2 eaten, 1 no-show, 1 cancelled); ratings 5 and 3. */
class AnalyticsIT extends AbstractIntegrationTest {

    @Autowired FeedbackRepository feedback;
    @Autowired ComplaintRepository complaints;

    User admin, staff, a, b, c;
    DailyMenu lunch;

    @BeforeEach
    void dataset() {
        admin = admin();
        staff = staff();
        a = student("B423001");
        b = student("B423002");
        c = student("B423003");
        student("B423004", ApprovalStatus.PENDING);
        lunch = menu(TODAY, MealType.LUNCH, true, "Dal Tadka", "Steamed Rice");
        booking(a, lunch, BookingStatus.CONSUMED);
        booking(b, lunch, BookingStatus.CONSUMED);
        booking(c, lunch, BookingStatus.NO_SHOW);
        booking(student("B423005"), lunch, BookingStatus.CANCELLED);
        rate(a, 5);
        rate(b, 3);
    }

    private void rate(User who, int rating) {
        Feedback f = new Feedback();
        f.setStudent(who);
        f.setDailyMenu(lunch);
        f.setRating((short) rating);
        feedback.save(f);
    }

    private void complaint(ComplaintStatus s) {
        Complaint x = new Complaint();
        x.setStudent(a);
        x.setCategory(ComplaintCategory.OTHER);
        x.setTitle("t");
        x.setDescription("d");
        x.setStatus(s);
        complaints.save(x);
    }

    @Test
    void overviewCountsMatchTheDataset() throws Exception {
        complaint(ComplaintStatus.OPEN);
        complaint(ComplaintStatus.IN_PROGRESS);
        complaint(ComplaintStatus.RESOLVED);
        post("/api/v1/bills/generate?month=2026-10", admin, null);   // 4 approved students x 300 fixed = 1200 outstanding

        get("/api/v1/analytics/overview", admin).andExpect(status().isOk())
                .andExpect(jsonPath("$.approvedStudents").value(4)).andExpect(jsonPath("$.pendingApprovals").value(1))
                .andExpect(jsonPath("$.todayBookings").value(3)).andExpect(jsonPath("$.todayServed").value(2))
                .andExpect(jsonPath("$.averageRating30d").value(4.0)).andExpect(jsonPath("$.openComplaints").value(2))
                .andExpect(jsonPath("$.outstanding").value(1200.0)).andExpect(jsonPath("$.revenueThisMonth").value(0.0));
    }

    @Test
    void revenueAppearsOnlyAfterPaymentAndIsSplitByBillMonth() throws Exception {
        post("/api/v1/bills/generate?month=2026-10", admin, null);
        long bill = bills.findMine(a.getId()).get(0).getId();
        post("/api/v1/payments/cash", admin, "{\"billId\":%d,\"amount\":300}".formatted(bill)).andExpect(status().isCreated());

        get("/api/v1/analytics/overview", admin).andExpect(jsonPath("$.revenueThisMonth").value(300.0)).andExpect(jsonPath("$.outstanding").value(900.0));
        get("/api/v1/analytics/revenue?months=3", admin).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[1].month").value("2026-10")).andExpect(jsonPath("$[1].billed").value(1200.0))
                .andExpect(jsonPath("$[1].collected").value(300.0)).andExpect(jsonPath("$[1].outstanding").value(900.0))
                .andExpect(jsonPath("$[1].bills").value(4)).andExpect(jsonPath("$[1].paidBills").value(1))
                .andExpect(jsonPath("$[2].month").value("2026-11")).andExpect(jsonPath("$[0].billed").value(0.0));
    }

    @Test
    void bookingsTrendFillsEveryDayAndSplitsByStatus() throws Exception {
        get("/api/v1/analytics/bookings-trend?days=7", staff).andExpect(status().isOk()).andExpect(jsonPath("$", hasSize(7)))
                .andExpect(jsonPath("$[0].date").value(TODAY.minusDays(6).toString())).andExpect(jsonPath("$[0].booked").value(0))
                .andExpect(jsonPath("$[6].date").value(TODAY.toString())).andExpect(jsonPath("$[6].booked").value(3))
                .andExpect(jsonPath("$[6].served").value(2)).andExpect(jsonPath("$[6].noShow").value(1)).andExpect(jsonPath("$[6].cancelled").value(1));
    }

    @Test
    void wastageIsTheShareOfResolvedBookingsThatWereNoShows() throws Exception {
        get("/api/v1/analytics/wastage", staff).andExpect(jsonPath("$.resolvedBookings").value(3)).andExpect(jsonPath("$.noShow").value(1))
                .andExpect(jsonPath("$.noShowRatePercent").value(33.33)).andExpect(jsonPath("$.lostValue").value(60.0))
                .andExpect(jsonPath("$.byMeal[1].mealType").value("LUNCH")).andExpect(jsonPath("$.byMeal[1].noShow").value(1))
                .andExpect(jsonPath("$.byMeal[0].noShowRatePercent").value(0.0));
    }

    @Test
    void mealDistributionSharesAddUpToOneHundredPercentOfServedMeals() throws Exception {
        get("/api/v1/analytics/meal-distribution", staff).andExpect(jsonPath("$", hasSize(4)))
                .andExpect(jsonPath("$[1].mealType").value("LUNCH")).andExpect(jsonPath("$[1].served").value(2))
                .andExpect(jsonPath("$[1].servedSharePercent").value(100.0)).andExpect(jsonPath("$[0].servedSharePercent").value(0.0));
    }

    @Test
    void ratingsTrendAndDishScoresUseMealRatings() throws Exception {
        get("/api/v1/analytics/ratings-trend?days=3", staff).andExpect(jsonPath("$", hasSize(3)))
                .andExpect(jsonPath("$[2].averageRating").value(4.0)).andExpect(jsonPath("$[2].count").value(2)).andExpect(jsonPath("$[0].averageRating").doesNotExist());
        get("/api/v1/analytics/top-dishes?limit=5", staff).andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].averageRating").value(4.0)).andExpect(jsonPath("$[0].ratings").value(2));
        get("/api/v1/analytics/top-dishes?minRatings=3", staff).andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void worstOrderingPutsLowestRatedDishesFirst() throws Exception {
        DailyMenu dinner = menu(TODAY.minusDays(1), MealType.DINNER, true, "Bhindi Fry");
        booking(a, dinner, BookingStatus.CONSUMED);
        Feedback f = new Feedback();
        f.setStudent(a);
        f.setDailyMenu(dinner);
        f.setRating((short) 1);
        feedback.save(f);
        get("/api/v1/analytics/top-dishes?order=worst&limit=1", staff).andExpect(jsonPath("$[0].name").value("Bhindi Fry")).andExpect(jsonPath("$[0].averageRating").value(1.0));
        get("/api/v1/analytics/top-dishes?order=best&limit=1", staff).andExpect(jsonPath("$[0].averageRating").value(4.0));
    }

    @Test
    void windowsAreClampedSoHugeRangesCannotBeRequested() throws Exception {
        get("/api/v1/analytics/bookings-trend?days=100000", staff).andExpect(jsonPath("$", hasSize(120)));
        get("/api/v1/analytics/bookings-trend?days=0", staff).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void attendanceCsvListsServedAndNoShowRowsOnly() throws Exception {
        String csv = get("/api/v1/reports/attendance.csv?from=%s&to=%s".formatted(TODAY, TODAY), staff).andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        org.assertj.core.api.Assertions.assertThat(csv.trim().split("\r\n")).hasSize(4);   // header + 2 eaten + 1 no-show (cancelled excluded)
        org.assertj.core.api.Assertions.assertThat(csv).contains("NO_SHOW").contains("CONSUMED").doesNotContain("CANCELLED");
        get("/api/v1/reports/attendance.csv?from=2026-01-01&to=2026-11-09", staff).andExpect(status().isBadRequest());   // > 92 days
    }
}
