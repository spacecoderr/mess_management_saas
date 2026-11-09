package com.thali.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import com.thali.TestProps;
import com.thali.billing.dto.GenerateBillsResponse;
import com.thali.booking.BookingRepository;
import com.thali.booking.NoShowService;
import com.thali.common.exception.ApiException;
import com.thali.support.Fixtures;
import com.thali.support.MutableClock;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;

/** Bill = consumed + no-show meals at their booked price + the fixed monthly charge (300.00 in these tests). */
class BillingServiceTest {

    static final YearMonth OCT = YearMonth.of(2026, 10);

    MutableClock clock = new MutableClock();   // Mon 9 Nov 2026
    BillRepository bills = mock(BillRepository.class);
    PaymentRepository payments = mock(PaymentRepository.class);
    BookingRepository bookings = mock(BookingRepository.class);
    StudentProfileRepository profiles = mock(StudentProfileRepository.class);
    NoShowService noShows = mock(NoShowService.class);
    User asha = Fixtures.user(1L, Role.STUDENT, "Asha Rao"), ben = Fixtures.user(2L, Role.STUDENT, "Ben Das");
    StudentProfile ashaProfile = Fixtures.profile(asha, "B423001"), benProfile = Fixtures.profile(ben, "B423002");

    BillingService service(String fixedCharge) {
        return new BillingService(bills, payments, bookings, profiles, noShows, TestProps.create(true, fixedCharge), clock);
    }

    @BeforeEach
    void setUp() {
        when(profiles.findAllApprovedActive()).thenReturn(List.of(ashaProfile, benProfile));
        when(bills.findByStudentIdAndBillMonth(any(), any())).thenReturn(Optional.empty());
        when(bills.save(any(Bill.class))).thenAnswer(i -> i.getArgument(0));
    }

    private void mealTotals(Object[]... rows) {
        when(bookings.billingTotals(any(), any(), anyCollection())).thenReturn(List.of(rows));
    }

    private List<Bill> savedBills(int expected) {
        ArgumentCaptor<Bill> cap = ArgumentCaptor.forClass(Bill.class);
        verify(bills, times(expected)).save(cap.capture());
        return cap.getAllValues();
    }

    @Test
    void totalIsMealChargesPlusFixedCharge() {
        mealTotals(new Object[] {1L, 3L, new BigDecimal("155.00")});   // Asha: 3 meals worth 155
        GenerateBillsResponse r = service("300.00").generate(OCT);

        assertThat(r.created()).isEqualTo(2);
        Bill a = savedBills(2).get(0);
        assertThat(a.getMealCount()).isEqualTo(3);
        assertThat(a.getMealCharges()).isEqualByComparingTo("155.00");
        assertThat(a.getFixedCharges()).isEqualByComparingTo("300.00");
        assertThat(a.getTotal()).isEqualByComparingTo("455.00");
        assertThat(a.getBillMonth()).isEqualTo(LocalDate.of(2026, 10, 1));
        assertThat(a.getStatus()).isEqualTo(BillStatus.UNPAID);
    }

    @Test
    void studentWithNoMealsStillPaysTheFixedCharge() {
        mealTotals(new Object[] {1L, 3L, new BigDecimal("155.00")});
        service("300.00").generate(OCT);
        Bill ben = savedBills(2).get(1);
        assertThat(ben.getMealCount()).isZero();
        assertThat(ben.getTotal()).isEqualByComparingTo("300.00");
    }

    @Test
    void zeroTotalMeansNoBillIsCreated() {
        mealTotals();   // nobody ate, and no fixed charge configured
        GenerateBillsResponse r = service("0.00").generate(OCT);
        assertThat(r.created()).isZero();
        verify(bills, never()).save(any());
    }

    @Test
    void paidBillsAreNeverTouchedByRegeneration() {
        mealTotals(new Object[] {1L, 2L, new BigDecimal("95.00")});
        Bill paid = new Bill();
        paid.setStatus(BillStatus.PAID);
        paid.setTotal(new BigDecimal("100.00"));
        when(bills.findByStudentIdAndBillMonth(1L, LocalDate.of(2026, 10, 1))).thenReturn(Optional.of(paid));

        GenerateBillsResponse r = service("300.00").generate(OCT);

        assertThat(r.skippedPaid()).isEqualTo(1);
        assertThat(r.created()).isEqualTo(1);   // Ben
        assertThat(paid.getTotal()).isEqualByComparingTo("100.00");
    }

    @Test
    void unpaidBillsAreRecalculatedInPlace() {
        mealTotals(new Object[] {1L, 4L, new BigDecimal("200.00")});
        Bill existing = new Bill();
        existing.setStatus(BillStatus.OVERDUE);
        existing.setTotal(new BigDecimal("100.00"));
        when(bills.findByStudentIdAndBillMonth(1L, LocalDate.of(2026, 10, 1))).thenReturn(Optional.of(existing));

        GenerateBillsResponse r = service("300.00").generate(OCT);

        assertThat(r.updated()).isEqualTo(1);
        assertThat(existing.getTotal()).isEqualByComparingTo("500.00");
        assertThat(existing.getStatus()).isEqualTo(BillStatus.OVERDUE);   // status is not reset
    }

    @Test
    void dueDateIsFifteenDaysAfterTheLaterOfMonthEndAndToday() {
        mealTotals();
        service("300.00").generate(OCT);                                                    // Oct ended -> today (9 Nov) + 15
        assertThat(savedBills(2).get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 11, 24));
    }

    @Test
    void dueDateForTheCurrentMonthCountsFromItsLastDay() {
        mealTotals();
        service("300.00").generate(YearMonth.of(2026, 11));                                 // 30 Nov + 15
        assertThat(savedBills(2).get(0).getDueDate()).isEqualTo(LocalDate.of(2026, 12, 15));
    }

    @Test
    void futureMonthsAreRefusedAndNothingIsSwept() {
        assertThatThrownBy(() -> service("300.00").generate(YearMonth.of(2026, 12)))
                .isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", "MONTH_IN_FUTURE");
        verify(noShows, never()).markNoShows();
    }

    @Test
    void noShowSweepRunsBeforeTotalsAreRead() {
        mealTotals();
        service("300.00").generate(OCT);
        var order = inOrder(noShows, bookings);
        order.verify(noShows).markNoShows();
        order.verify(bookings).billingTotals(any(), any(), anyCollection());
    }
}
