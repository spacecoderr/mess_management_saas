package com.thali.it;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.thali.billing.Bill;
import com.thali.billing.BillStatus;
import com.thali.billing.BillingService;
import com.thali.booking.BookingStatus;
import com.thali.menu.DailyMenu;
import com.thali.menu.MealType;
import com.thali.user.User;

/** October 2026 history is built directly; "now" is 9 Nov 2026, so October is a finished month. Fixed charge is 300. */
class BillingFlowIT extends AbstractIntegrationTest {

    @Autowired BillingService billingService;

    User admin, asha, ben;

    @BeforeEach
    void people() {
        admin = admin();
        asha = student("B423001");
        ben = student("B423002");
    }

    private DailyMenu oct(int day, MealType type) {
        return menu(LocalDate.of(2026, 10, day), type, true, "Dal");
    }

    /** Asha: 3 lunches eaten (180) + 1 no-show breakfast (35) + 1 cancelled dinner (free) + a stale still-BOOKED snack (20, swept to no-show). */
    private void ashaOctoberHistory() {
        booking(asha, oct(5, MealType.LUNCH), BookingStatus.CONSUMED);
        booking(asha, oct(6, MealType.LUNCH), BookingStatus.CONSUMED);
        booking(asha, oct(7, MealType.LUNCH), BookingStatus.CONSUMED);
        booking(asha, oct(5, MealType.BREAKFAST), BookingStatus.NO_SHOW);
        booking(asha, oct(5, MealType.DINNER), BookingStatus.CANCELLED);
        booking(asha, oct(6, MealType.SNACKS), BookingStatus.BOOKED);
    }

    private long billId(User student) {
        return bills.findMine(student.getId()).get(0).getId();
    }

    @Test
    void billCountsEatenAndNoShowMealsPlusTheFixedCharge() throws Exception {
        ashaOctoberHistory();

        post("/api/v1/bills/generate?month=2026-10", admin, null).andExpect(status().isOk())
                .andExpect(jsonPath("$.created").value(2)).andExpect(jsonPath("$.updated").value(0)).andExpect(jsonPath("$.skippedPaid").value(0));

        get("/api/v1/bills/" + billId(asha), asha).andExpect(status().isOk())
                .andExpect(jsonPath("$.month").value("2026-10")).andExpect(jsonPath("$.mealCount").value(5))
                .andExpect(jsonPath("$.mealCharges").value(235.0)).andExpect(jsonPath("$.fixedCharges").value(300.0))
                .andExpect(jsonPath("$.total").value(535.0)).andExpect(jsonPath("$.status").value("UNPAID"))
                .andExpect(jsonPath("$.lines", hasSize(5))).andExpect(jsonPath("$.lines[0].date").value("2026-10-05"));
        get("/api/v1/bills/" + billId(ben), ben).andExpect(jsonPath("$.mealCount").value(0)).andExpect(jsonPath("$.total").value(300.0));
    }

    @Test
    void theStaleBookedMealIsSweptToNoShowSoItIsBilledNotLost() throws Exception {
        ashaOctoberHistory();
        post("/api/v1/bills/generate?month=2026-10", admin, null).andExpect(status().isOk());
        assertThat(bookings.findForStudent(asha.getId(), LocalDate.of(2026, 10, 6), LocalDate.of(2026, 10, 6)))
                .filteredOn(b -> b.getDailyMenu().getMealType() == MealType.SNACKS)
                .singleElement().extracting(b -> b.getStatus()).isEqualTo(BookingStatus.NO_SHOW);
    }

    @Test
    void regeneratingIsIdempotentAndPicksUpNewMealsForUnpaidBills() throws Exception {
        booking(ben, oct(5, MealType.LUNCH), BookingStatus.CONSUMED);
        post("/api/v1/bills/generate?month=2026-10", admin, null).andExpect(jsonPath("$.created").value(2));
        post("/api/v1/bills/generate?month=2026-10", admin, null).andExpect(jsonPath("$.created").value(0)).andExpect(jsonPath("$.updated").value(2));
        assertThat(bills.count()).isEqualTo(2);

        booking(ben, oct(6, MealType.LUNCH), BookingStatus.CONSUMED);
        post("/api/v1/bills/generate?month=2026-10", admin, null).andExpect(status().isOk());
        get("/api/v1/bills/" + billId(ben), ben).andExpect(jsonPath("$.total").value(420.0));   // 2 x 60 + 300
    }

    @Test
    void studentPaysOnlineThenRegenerationLeavesThePaidBillUntouched() throws Exception {
        ashaOctoberHistory();
        post("/api/v1/bills/generate?month=2026-10", admin, null);
        long bill = billId(asha);

        long payment = readId(post("/api/v1/payments/checkout", asha, "{\"billId\":%d,\"method\":\"UPI\"}".formatted(bill))
                .andExpect(status().isCreated()).andExpect(jsonPath("$.status").value("PENDING")).andExpect(jsonPath("$.amount").value(535.0))
                .andExpect(jsonPath("$.gatewayPayload").value(org.hamcrest.Matchers.startsWith("upi://pay"))));
        get("/api/v1/bills/" + bill, asha).andExpect(jsonPath("$.status").value("UNPAID"));   // not paid until confirmed

        post("/api/v1/payments/%d/confirm".formatted(payment), ben, null).andExpect(status().isNotFound());   // not Ben's payment
        post("/api/v1/payments/%d/confirm".formatted(payment), asha, null).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("SUCCESS"));
        post("/api/v1/payments/%d/confirm".formatted(payment), asha, null).andExpect(status().isOk());   // idempotent
        get("/api/v1/bills/" + bill, asha).andExpect(jsonPath("$.status").value("PAID")).andExpect(jsonPath("$.paidAt").isNotEmpty());

        post("/api/v1/payments/checkout", asha, "{\"billId\":%d,\"method\":\"CARD\"}".formatted(bill)).andExpect(status().isConflict()).andExpect(jsonPath("$.code").value("BILL_ALREADY_PAID"));
        booking(asha, oct(8, MealType.LUNCH), BookingStatus.CONSUMED);   // a late-arriving meal must not change a paid bill
        post("/api/v1/bills/generate?month=2026-10", admin, null).andExpect(jsonPath("$.skippedPaid").value(1)).andExpect(jsonPath("$.updated").value(1));
        get("/api/v1/bills/" + bill, asha).andExpect(jsonPath("$.total").value(535.0));
        get("/api/v1/payments/me", asha).andExpect(jsonPath("$", hasSize(1)));
    }

    @Test
    void anAbandonedCheckoutIsReplacedByTheNewOne() throws Exception {
        post("/api/v1/bills/generate?month=2026-10", admin, null);
        long bill = billId(asha);
        long first = readId(post("/api/v1/payments/checkout", asha, "{\"billId\":%d,\"method\":\"UPI\"}".formatted(bill)));
        long second = readId(post("/api/v1/payments/checkout", asha, "{\"billId\":%d,\"method\":\"CARD\"}".formatted(bill)));
        post("/api/v1/payments/%d/confirm".formatted(first), asha, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("PAYMENT_NOT_PENDING"));
        post("/api/v1/payments/%d/confirm".formatted(second), asha, null).andExpect(status().isOk());
    }

    @Test
    void cashIsRecordedByAdminForTheExactAmountOnly() throws Exception {
        post("/api/v1/bills/generate?month=2026-10", admin, null);
        long bill = billId(ben);
        post("/api/v1/payments/cash", admin, "{\"billId\":%d,\"amount\":299.99}".formatted(bill)).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("AMOUNT_MISMATCH"));
        post("/api/v1/payments/cash", admin, "{\"billId\":%d,\"amount\":300}".formatted(bill)).andExpect(status().isCreated())
                .andExpect(jsonPath("$.method").value("CASH")).andExpect(jsonPath("$.recordedBy").value("Test Admin"));
        get("/api/v1/bills/" + bill, admin).andExpect(jsonPath("$.status").value("PAID"));
        post("/api/v1/payments/cash", admin, "{\"billId\":%d,\"amount\":300}".formatted(bill)).andExpect(status().isConflict());
        post("/api/v1/payments/checkout", ben, "{\"billId\":%d,\"method\":\"UPI\"}".formatted(bill)).andExpect(status().isConflict());
    }

    @Test
    void studentsOnlySeeTheirOwnBills() throws Exception {
        post("/api/v1/bills/generate?month=2026-10", admin, null);
        get("/api/v1/bills/" + billId(asha), ben).andExpect(status().isNotFound());
        post("/api/v1/payments/checkout", ben, "{\"billId\":%d,\"method\":\"UPI\"}".formatted(billId(asha))).andExpect(status().isNotFound());
        get("/api/v1/bills/me", ben).andExpect(jsonPath("$", hasSize(1))).andExpect(jsonPath("$[0].studentId").value(ben.getId()));
        get("/api/v1/bills/" + billId(asha), admin).andExpect(status().isOk());
    }

    @Test
    void futureMonthsAndBadMonthFormatsAreRejected() throws Exception {
        post("/api/v1/bills/generate?month=2026-12", admin, null).andExpect(status().isUnprocessableEntity()).andExpect(jsonPath("$.code").value("MONTH_IN_FUTURE"));
        post("/api/v1/bills/generate?month=november", admin, null).andExpect(status().isBadRequest());
        post("/api/v1/bills/generate", admin, null).andExpect(status().isBadRequest());
    }

    @Test
    void onlyApprovedActiveStudentsAreBilled() throws Exception {
        student("B423003", com.thali.user.ApprovalStatus.PENDING);
        User off = student("B423004");
        off.setActive(false);
        users.save(off);
        post("/api/v1/bills/generate?month=2026-10", admin, null).andExpect(jsonPath("$.created").value(2));   // Asha + Ben only
    }

    @Test
    void adminCanFilterBillsByMonthAndStatus() throws Exception {
        booking(ben, oct(5, MealType.LUNCH), BookingStatus.CONSUMED);
        post("/api/v1/bills/generate?month=2026-10", admin, null);
        post("/api/v1/payments/cash", admin, "{\"billId\":%d,\"amount\":360}".formatted(billId(ben))).andExpect(status().isCreated());

        get("/api/v1/bills?month=2026-10", admin).andExpect(jsonPath("$.totalElements").value(2));
        get("/api/v1/bills?month=2026-10&status=PAID", admin).andExpect(jsonPath("$.totalElements").value(1)).andExpect(jsonPath("$.content[0].rollNo").value("B423002"));
        get("/api/v1/bills?month=2026-09", admin).andExpect(jsonPath("$.totalElements").value(0));
        get("/api/v1/bills", admin).andExpect(jsonPath("$.totalElements").value(2));   // no filters at all
    }

    @Test
    void overdueSweepFlipsUnpaidBillsPastDueAndTheyCanStillBePaid() throws Exception {
        post("/api/v1/bills/generate?month=2026-10", admin, null);
        long bill = billId(asha);
        jdbc.update("UPDATE bills SET due_date = ? WHERE id = ?", java.sql.Date.valueOf("2026-11-01"), bill);
        assertThat(billingService.markOverdue()).isEqualTo(1);
        assertThat(bills.findById(bill).orElseThrow().getStatus()).isEqualTo(BillStatus.OVERDUE);

        long pay = readId(post("/api/v1/payments/checkout", asha, "{\"billId\":%d,\"method\":\"UPI\"}".formatted(bill)).andExpect(status().isCreated()));
        post("/api/v1/payments/%d/confirm".formatted(pay), asha, null).andExpect(status().isOk());
        Bill after = bills.findById(bill).orElseThrow();
        assertThat(after.getStatus()).isEqualTo(BillStatus.PAID);
        assertThat(billingService.markOverdue()).isZero();   // paid bills are never flagged
    }

    @Test
    void csvExportContainsTheBillsAndIsSafeAgainstFormulaInjection() throws Exception {
        asha.setName("=HYPERLINK(\"http://evil\")");
        users.save(asha);
        booking(ben, oct(5, MealType.LUNCH), BookingStatus.CONSUMED);
        post("/api/v1/bills/generate?month=2026-10", admin, null);

        String csv = get("/api/v1/reports/bills.csv?month=2026-10", admin).andExpect(status().isOk())
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.header().string("Content-Disposition", org.hamcrest.Matchers.containsString("bills-2026-10.csv")))
                .andReturn().getResponse().getContentAsString(java.nio.charset.StandardCharsets.UTF_8);
        assertThat(csv).contains("roll_no,name,email,month,meals,meal_charges,fixed_charges,total,status,due_date");
        assertThat(csv).contains("B423002").contains("360.00").contains("B423001");
        assertThat(csv).contains("\"'=HYPERLINK(\"\"http://evil\"\")\"");   // quoted and neutralised with a leading '
    }
}
