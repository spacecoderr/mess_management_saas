package com.thali.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.List;
import java.util.UUID;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.billing.dto.PaymentResponse;
import com.thali.common.PageResponse;
import com.thali.common.exception.ApiException;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;
import com.thali.user.UserRepository;

/** Online payments go checkout (PENDING) -> confirm (SUCCESS); cash is recorded by an admin in one step. */
@Service
public class PaymentService {

    private final PaymentRepository payments;
    private final BillRepository bills;
    private final PaymentGateway gateway;
    private final StudentProfileRepository profiles;
    private final UserRepository users;
    private final Clock clock;

    public PaymentService(PaymentRepository payments, BillRepository bills, PaymentGateway gateway,
                          StudentProfileRepository profiles, UserRepository users, Clock clock) {
        this.payments = payments;
        this.bills = bills;
        this.gateway = gateway;
        this.profiles = profiles;
        this.users = users;
        this.clock = clock;
    }

    @Transactional
    public PaymentResponse checkout(Long studentId, Long billId, PaymentMethod method) {
        if (method == PaymentMethod.CASH) {
            throw ApiException.badRequest("INVALID_METHOD", "Cash payments are recorded at the mess office.");
        }
        Bill bill = ownedBill(studentId, billId);
        requirePayable(bill);
        // only one live attempt per bill: older pending attempts are abandoned
        payments.findByBillIdAndStatus(billId, PaymentStatus.PENDING).forEach(p -> p.setStatus(PaymentStatus.FAILED));

        var order = gateway.createOrder(bill.getTotal(), method, "Mess bill " + YearMonth.from(bill.getBillMonth()));
        Payment p = new Payment();
        p.setBill(bill);
        p.setAmount(bill.getTotal());
        p.setMethod(method);
        p.setTxnRef(order.txnRef());
        p.setStatus(PaymentStatus.PENDING);
        payments.save(p);
        return PaymentResponse.of(p, rollNo(studentId), order.payload());
    }

    @Transactional
    public PaymentResponse confirm(Long studentId, Long paymentId) {
        Payment p = payments.findDetailed(paymentId).orElseThrow(this::notFound);
        if (!p.getBill().getStudent().getId().equals(studentId)) throw notFound();
        if (p.getStatus() == PaymentStatus.SUCCESS) return PaymentResponse.of(p, rollNo(studentId), null);   // idempotent
        if (p.getStatus() != PaymentStatus.PENDING) {
            throw ApiException.unprocessable("PAYMENT_NOT_PENDING", "This payment attempt is no longer active. Start a new one.");
        }
        Bill bill = p.getBill();
        if (bill.getStatus() == BillStatus.PAID) {
            p.setStatus(PaymentStatus.FAILED);
            throw ApiException.conflict("BILL_ALREADY_PAID", "This bill has already been paid.");
        }
        if (p.getAmount().compareTo(bill.getTotal()) != 0) {
            p.setStatus(PaymentStatus.FAILED);
            throw ApiException.conflict("BILL_CHANGED", "The bill amount changed. Please start a new payment.");
        }
        if (!gateway.confirm(p.getTxnRef())) {
            p.setStatus(PaymentStatus.FAILED);
            throw ApiException.unprocessable("PAYMENT_FAILED", "The payment could not be completed.");
        }
        p.setStatus(PaymentStatus.SUCCESS);
        p.setPaidAt(clock.instant());
        bill.setStatus(BillStatus.PAID);
        return PaymentResponse.of(p, rollNo(studentId), null);
    }

    @Transactional
    public PaymentResponse recordCash(Long adminId, Long billId, BigDecimal amount) {
        Bill bill = bills.findDetailed(billId).orElseThrow(() -> ApiException.notFound("BILL_NOT_FOUND", "Bill not found."));
        requirePayable(bill);
        if (amount.compareTo(bill.getTotal()) != 0) {
            throw ApiException.unprocessable("AMOUNT_MISMATCH", "Cash must equal the bill total of " + bill.getTotal() + ".");
        }
        payments.findByBillIdAndStatus(billId, PaymentStatus.PENDING).forEach(x -> x.setStatus(PaymentStatus.FAILED));
        Payment p = new Payment();
        p.setBill(bill);
        p.setAmount(amount);
        p.setMethod(PaymentMethod.CASH);
        p.setTxnRef("CASH-" + UUID.randomUUID().toString().replace("-", "").substring(0, 12).toUpperCase());
        p.setStatus(PaymentStatus.SUCCESS);
        p.setPaidAt(clock.instant());
        p.setRecordedBy(users.getReferenceById(adminId));
        payments.save(p);
        bill.setStatus(BillStatus.PAID);
        return PaymentResponse.of(payments.findDetailed(p.getId()).orElse(p), rollNo(bill.getStudent().getId()), null);
    }

    @Transactional(readOnly = true)
    public PageResponse<PaymentResponse> search(LocalDate from, LocalDate to, PaymentStatus status, int page, int size) {
        var statuses = status == null ? EnumSet.allOf(PaymentStatus.class) : EnumSet.of(status);
        Instant start = from.atStartOfDay(clock.getZone()).toInstant();
        Instant end = to.plusDays(1).atStartOfDay(clock.getZone()).toInstant();
        var result = payments.search(start, end, statuses, PageRequest.of(page, Math.max(1, Math.min(size, 100)), Sort.by("createdAt").descending()));
        var rolls = profiles.findAllById(result.getContent().stream().map(p -> p.getBill().getStudent().getId()).distinct().toList())
                .stream().collect(java.util.stream.Collectors.toMap(StudentProfile::getUserId, StudentProfile::getRollNo));
        return PageResponse.of(result, p -> PaymentResponse.of(p, rolls.get(p.getBill().getStudent().getId()), null));
    }

    @Transactional(readOnly = true)
    public List<PaymentResponse> mine(Long studentId) {
        String roll = rollNo(studentId);
        return payments.findMine(studentId).stream().map(p -> PaymentResponse.of(p, roll, null)).toList();
    }

    // ------------------------------------------------------------------ internals

    private Bill ownedBill(Long studentId, Long billId) {
        Bill b = bills.findDetailed(billId).orElseThrow(() -> ApiException.notFound("BILL_NOT_FOUND", "Bill not found."));
        if (!b.getStudent().getId().equals(studentId)) throw ApiException.notFound("BILL_NOT_FOUND", "Bill not found.");
        return b;
    }

    private void requirePayable(Bill bill) {
        if (bill.getStatus() == BillStatus.PAID) throw ApiException.conflict("BILL_ALREADY_PAID", "This bill has already been paid.");
    }

    private String rollNo(Long studentId) {
        return profiles.findById(studentId).map(StudentProfile::getRollNo).orElse(null);
    }

    private ApiException notFound() {
        return ApiException.notFound("PAYMENT_NOT_FOUND", "Payment not found.");
    }
}
