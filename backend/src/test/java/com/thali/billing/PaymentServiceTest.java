package com.thali.billing;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.thali.billing.dto.PaymentResponse;
import com.thali.common.exception.ApiException;
import com.thali.support.Fixtures;
import com.thali.support.MutableClock;
import com.thali.user.Role;
import com.thali.user.StudentProfileRepository;
import com.thali.user.User;
import com.thali.user.UserRepository;

class PaymentServiceTest {

    MutableClock clock = new MutableClock();
    PaymentRepository payments = mock(PaymentRepository.class);
    BillRepository bills = mock(BillRepository.class);
    StudentProfileRepository profiles = mock(StudentProfileRepository.class);
    UserRepository users = mock(UserRepository.class);
    User student = Fixtures.user(7L, Role.STUDENT, "Asha Rao"), admin = Fixtures.user(1L, Role.ADMIN, "Mess Admin");
    Bill bill;
    PaymentService service;

    @BeforeEach
    void setUp() {
        service = new PaymentService(payments, bills, new MockPaymentGateway(), profiles, users, clock);
        bill = new Bill();
        bill.setId(10L);
        bill.setStudent(student);
        bill.setBillMonth(LocalDate.of(2026, 10, 1));
        bill.setTotal(new BigDecimal("455.00"));
        bill.setStatus(BillStatus.UNPAID);
        when(bills.findDetailed(10L)).thenReturn(Optional.of(bill));
        when(payments.save(any(Payment.class))).thenAnswer(i -> i.getArgument(0));
        when(users.getReferenceById(1L)).thenReturn(admin);
    }

    private static void assertCode(Runnable call, String code) {
        assertThatThrownBy(call::run).isInstanceOf(ApiException.class).hasFieldOrPropertyWithValue("code", code);
    }

    private Payment pending(BigDecimal amount) {
        Payment p = new Payment();
        p.setId(5L);
        p.setBill(bill);
        p.setAmount(amount);
        p.setMethod(PaymentMethod.UPI);
        p.setTxnRef("MOCK-ABC123");
        p.setStatus(PaymentStatus.PENDING);
        when(payments.findDetailed(5L)).thenReturn(Optional.of(p));
        return p;
    }

    @Test
    void checkoutCreatesAPendingPaymentForTheFullBillAmount() {
        PaymentResponse r = service.checkout(7L, 10L, PaymentMethod.UPI);
        assertThat(r.status()).isEqualTo(PaymentStatus.PENDING);
        assertThat(r.amount()).isEqualByComparingTo("455.00");
        assertThat(r.gatewayPayload()).startsWith("upi://pay").contains("am=455.00");
        assertThat(bill.getStatus()).isEqualTo(BillStatus.UNPAID);   // nothing is paid until confirm
    }

    @Test
    void aNewCheckoutAbandonsEarlierPendingAttempts() {
        Payment old = new Payment();
        old.setStatus(PaymentStatus.PENDING);
        when(payments.findByBillIdAndStatus(10L, PaymentStatus.PENDING)).thenReturn(List.of(old));
        service.checkout(7L, 10L, PaymentMethod.CARD);
        assertThat(old.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void cashCannotBePaidOnline() {
        assertCode(() -> service.checkout(7L, 10L, PaymentMethod.CASH), "INVALID_METHOD");
    }

    @Test
    void aStudentCannotPayAnotherStudentsBill() {
        assertCode(() -> service.checkout(99L, 10L, PaymentMethod.UPI), "BILL_NOT_FOUND");
    }

    @Test
    void aPaidBillCannotBePaidAgain() {
        bill.setStatus(BillStatus.PAID);
        assertCode(() -> service.checkout(7L, 10L, PaymentMethod.UPI), "BILL_ALREADY_PAID");
    }

    @Test
    void confirmMarksThePaymentSuccessfulAndTheBillPaid() {
        Payment p = pending(new BigDecimal("455.00"));
        PaymentResponse r = service.confirm(7L, 5L);
        assertThat(r.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(p.getPaidAt()).isEqualTo(clock.instant());
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
    }

    @Test
    void confirmingTwiceIsHarmless() {
        pending(new BigDecimal("455.00"));
        service.confirm(7L, 5L);
        assertThat(service.confirm(7L, 5L).status()).isEqualTo(PaymentStatus.SUCCESS);
    }

    @Test
    void aStudentCannotConfirmSomeoneElsesPayment() {
        pending(new BigDecimal("455.00"));
        assertCode(() -> service.confirm(99L, 5L), "PAYMENT_NOT_FOUND");
        assertThat(bill.getStatus()).isEqualTo(BillStatus.UNPAID);
    }

    @Test
    void ifTheBillWasRecalculatedMeanwhileTheOldPaymentFails() {
        Payment p = pending(new BigDecimal("400.00"));   // bill is now 455
        assertCode(() -> service.confirm(7L, 5L), "BILL_CHANGED");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
        assertThat(bill.getStatus()).isEqualTo(BillStatus.UNPAID);
    }

    @Test
    void ifTheBillWasPaidByCashMeanwhileTheOnlinePaymentFails() {
        Payment p = pending(new BigDecimal("455.00"));
        bill.setStatus(BillStatus.PAID);
        assertCode(() -> service.confirm(7L, 5L), "BILL_ALREADY_PAID");
        assertThat(p.getStatus()).isEqualTo(PaymentStatus.FAILED);
    }

    @Test
    void cashMustMatchTheBillTotalExactly() {
        assertCode(() -> service.recordCash(1L, 10L, new BigDecimal("400.00")), "AMOUNT_MISMATCH");
        verify(payments, never()).save(any());
        assertThat(bill.getStatus()).isEqualTo(BillStatus.UNPAID);
    }

    @Test
    void recordingCashMarksTheBillPaidAndRemembersTheAdmin() {
        PaymentResponse r = service.recordCash(1L, 10L, new BigDecimal("455.00"));
        assertThat(r.method()).isEqualTo(PaymentMethod.CASH);
        assertThat(r.status()).isEqualTo(PaymentStatus.SUCCESS);
        assertThat(r.recordedBy()).isEqualTo("Mess Admin");
        assertThat(bill.getStatus()).isEqualTo(BillStatus.PAID);
    }
}
