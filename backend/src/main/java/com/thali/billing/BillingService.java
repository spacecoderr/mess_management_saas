package com.thali.billing;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.billing.dto.BillLine;
import com.thali.billing.dto.BillResponse;
import com.thali.billing.dto.GenerateBillsResponse;
import com.thali.booking.BookingRepository;
import com.thali.booking.BookingStatus;
import com.thali.booking.NoShowService;
import com.thali.common.PageResponse;
import com.thali.common.exception.ApiException;
import com.thali.config.AppProperties;
import com.thali.user.Role;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;

/** Bill = (consumed + no-show meals at their booked price) + fixed monthly charge. */
@Service
public class BillingService {

    private static final Logger log = LoggerFactory.getLogger(BillingService.class);
    private static final Set<BookingStatus> BILLABLE = EnumSet.of(BookingStatus.CONSUMED, BookingStatus.NO_SHOW);

    private final BillRepository bills;
    private final PaymentRepository payments;
    private final BookingRepository bookings;
    private final StudentProfileRepository profiles;
    private final NoShowService noShows;
    private final AppProperties.Billing cfg;
    private final Clock clock;

    public BillingService(BillRepository bills, PaymentRepository payments, BookingRepository bookings,
                          StudentProfileRepository profiles, NoShowService noShows, AppProperties props, Clock clock) {
        this.bills = bills;
        this.payments = payments;
        this.bookings = bookings;
        this.profiles = profiles;
        this.noShows = noShows;
        this.cfg = props.billing();
        this.clock = clock;
    }

    /** Idempotent: unpaid bills are recalculated, paid bills are never touched. */
    @Transactional
    public GenerateBillsResponse generate(YearMonth month) {
        if (month.isAfter(YearMonth.now(clock))) {
            throw ApiException.unprocessable("MONTH_IN_FUTURE", "Bills can't be generated for a future month.");
        }
        noShows.markNoShows();   // make sure finished-but-unscanned meals are counted

        LocalDate first = month.atDay(1), last = month.atEndOfMonth();
        Map<Long, Object[]> totals = new HashMap<>();
        for (Object[] row : bookings.billingTotals(first, last, BILLABLE)) totals.put((Long) row[0], row);

        LocalDate due = LocalDate.now(clock).isAfter(last) ? LocalDate.now(clock) : last;
        due = due.plusDays(cfg.dueDays());

        int created = 0, updated = 0, skipped = 0;
        for (StudentProfile sp : profiles.findAllApprovedActive()) {
            Object[] t = totals.get(sp.getUserId());
            int count = t == null ? 0 : ((Number) t[1]).intValue();
            BigDecimal mealCharges = t == null ? BigDecimal.ZERO : (BigDecimal) t[2];
            BigDecimal total = mealCharges.add(cfg.fixedMonthlyCharge());
            if (total.signum() == 0) continue;

            Bill bill = bills.findByStudentIdAndBillMonth(sp.getUserId(), first).orElse(null);
            if (bill != null && bill.getStatus() == BillStatus.PAID) { skipped++; continue; }
            if (bill == null) {
                bill = new Bill();
                bill.setStudent(sp.getUser());
                bill.setBillMonth(first);
                bill.setDueDate(due);
                created++;
            } else {
                updated++;
            }
            bill.setMealCount(count);
            bill.setMealCharges(mealCharges);
            bill.setFixedCharges(cfg.fixedMonthlyCharge());
            bill.setTotal(total);
            bill.setGeneratedAt(clock.instant());
            bills.save(bill);
        }
        log.info("Bills for {}: {} created, {} updated, {} already paid", month, created, updated, skipped);
        return new GenerateBillsResponse(month.toString(), created, updated, skipped);
    }

    @Transactional(readOnly = true)
    public PageResponse<BillResponse> search(YearMonth month, BillStatus status, int page, int size) {
        var statuses = status == null ? EnumSet.allOf(BillStatus.class) : EnumSet.of(status);
        LocalDate from = month == null ? LocalDate.of(2000, 1, 1) : month.atDay(1);
        LocalDate to = month == null ? LocalDate.of(2999, 12, 1) : month.atDay(1);
        var result = bills.search(from, to, statuses,
                PageRequest.of(page, Math.max(1, Math.min(size, 100)), Sort.by("billMonth").descending().and(Sort.by("student.name"))));
        Map<Long, String> rolls = rollNos(result.getContent().stream().map(b -> b.getStudent().getId()).distinct().toList());
        return PageResponse.of(result, b -> BillResponse.of(b, rolls.get(b.getStudent().getId()), null, null));
    }

    @Transactional(readOnly = true)
    public List<BillResponse> mine(Long studentId) {
        return bills.findMine(studentId).stream().map(b -> BillResponse.of(b, null, null, null)).toList();
    }

    @Transactional(readOnly = true)
    public BillResponse get(Long id, Long userId, Role role) {
        Bill b = bills.findDetailed(id).orElseThrow(() -> ApiException.notFound("BILL_NOT_FOUND", "Bill not found."));
        if (role == Role.STUDENT && !b.getStudent().getId().equals(userId)) {
            throw ApiException.notFound("BILL_NOT_FOUND", "Bill not found.");
        }
        YearMonth ym = YearMonth.from(b.getBillMonth());
        List<BillLine> lines = bookings.findBillable(b.getStudent().getId(), ym.atDay(1), ym.atEndOfMonth(), BILLABLE).stream()
                .map(k -> new BillLine(k.getDailyMenu().getMenuDate(), k.getDailyMenu().getMealType(), k.getStatus(), k.getPriceAtBooking()))
                .toList();
        Instant paidAt = payments.findByBillIdAndStatus(id, PaymentStatus.SUCCESS).stream()
                .map(Payment::getPaidAt).max(Instant::compareTo).orElse(null);
        String roll = profiles.findById(b.getStudent().getId()).map(StudentProfile::getRollNo).orElse(null);
        return BillResponse.of(b, roll, paidAt, lines);
    }

    @Transactional
    public int markOverdue() {
        int n = bills.markOverdue(LocalDate.now(clock));
        if (n > 0) log.info("Marked {} bills overdue", n);
        return n;
    }

    private Map<Long, String> rollNos(List<Long> ids) {
        return profiles.findAllById(ids).stream().collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getRollNo));
    }
}
