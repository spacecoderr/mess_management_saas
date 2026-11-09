package com.thali.analytics;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.analytics.AnalyticsDtos.DayPoint;
import com.thali.analytics.AnalyticsDtos.DishScore;
import com.thali.analytics.AnalyticsDtos.MealShare;
import com.thali.analytics.AnalyticsDtos.MealWastage;
import com.thali.analytics.AnalyticsDtos.Overview;
import com.thali.analytics.AnalyticsDtos.RatingPoint;
import com.thali.analytics.AnalyticsDtos.RevenuePoint;
import com.thali.analytics.AnalyticsDtos.Wastage;
import com.thali.billing.BillStatus;
import com.thali.billing.PaymentStatus;
import com.thali.booking.BookingStatus;
import com.thali.complaint.ComplaintStatus;
import com.thali.menu.MealType;
import com.thali.user.ApprovalStatus;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import jakarta.persistence.Query;

/** Read-only aggregate queries (JPQL) behind the admin dashboard. All "days" windows end today, campus time. */
@Service
@Transactional(readOnly = true)
public class AnalyticsService {

    @PersistenceContext
    private EntityManager em;

    private final Clock clock;

    public AnalyticsService(Clock clock) {
        this.clock = clock;
    }

    public Overview overview() {
        LocalDate today = LocalDate.now(clock);
        var monthStart = YearMonth.from(today).atDay(1).atStartOfDay(clock.getZone()).toInstant();
        long approved = count("select count(sp) from StudentProfile sp where sp.approvalStatus = :s", "s", ApprovalStatus.APPROVED);
        long pending = count("select count(sp) from StudentProfile sp where sp.approvalStatus = :s", "s", ApprovalStatus.PENDING);
        long todayBookings = count("select count(b) from Booking b join b.dailyMenu dm where dm.menuDate = :d and b.status <> :s",
                "d", today, "s", BookingStatus.CANCELLED);
        long todayServed = count("select count(b) from Booking b join b.dailyMenu dm where dm.menuDate = :d and b.status = :s",
                "d", today, "s", BookingStatus.CONSUMED);
        BigDecimal revenue = money(one("select sum(p.amount) from Payment p where p.status = :s and p.paidAt >= :from",
                "s", PaymentStatus.SUCCESS, "from", monthStart));
        BigDecimal outstanding = money(one("select sum(b.total) from Bill b where b.status <> :s", "s", BillStatus.PAID));
        long overdue = count("select count(b) from Bill b where b.status = :s", "s", BillStatus.OVERDUE);
        Object avg = one("select avg(f.rating) from Feedback f join f.dailyMenu dm where dm.menuDate between :from and :to",
                "from", today.minusDays(29), "to", today);
        long open = count("select count(c) from Complaint c where c.status in :s", "s", List.of(ComplaintStatus.OPEN, ComplaintStatus.IN_PROGRESS));
        return new Overview(approved, pending, todayBookings, todayServed, revenue, outstanding, overdue,
                avg == null ? null : round(((Number) avg).doubleValue()), open);
    }

    public List<DayPoint> bookingsTrend(int days) {
        int n = clampDays(days);
        LocalDate to = LocalDate.now(clock), from = to.minusDays(n - 1L);
        Map<LocalDate, long[]> byDay = new HashMap<>();   // [booked, served, noShow, cancelled]
        for (Object[] r : rows("select dm.menuDate, b.status, count(b) from Booking b join b.dailyMenu dm "
                + "where dm.menuDate between :from and :to group by dm.menuDate, b.status", "from", from, "to", to)) {
            long[] a = byDay.computeIfAbsent((LocalDate) r[0], k -> new long[4]);
            long c = ((Number) r[2]).longValue();
            switch ((BookingStatus) r[1]) {
                case CONSUMED -> { a[0] += c; a[1] += c; }
                case NO_SHOW -> { a[0] += c; a[2] += c; }
                case BOOKED -> a[0] += c;
                case CANCELLED -> a[3] += c;
            }
        }
        List<DayPoint> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            long[] a = byDay.getOrDefault(d, new long[4]);
            out.add(new DayPoint(d, a[0], a[1], a[2], a[3]));
        }
        return out;
    }

    public List<MealShare> mealDistribution(int days) {
        Map<MealType, long[]> byMeal = statusCounts(days);
        long totalServed = byMeal.values().stream().mapToLong(a -> a[1]).sum();
        return byMeal.entrySet().stream().map(e -> {
            long[] a = e.getValue();
            return new MealShare(e.getKey(), a[0], a[1], a[2], a[3], totalServed == 0 ? 0 : round(100.0 * a[1] / totalServed));
        }).toList();
    }

    public Wastage wastage(int days) {
        int n = clampDays(days);
        LocalDate to = LocalDate.now(clock), from = to.minusDays(n - 1L);
        Map<MealType, long[]> counts = new EnumMap<>(MealType.class);   // [resolved, noShow]
        Map<MealType, BigDecimal> lost = new EnumMap<>(MealType.class);
        for (Object[] r : rows("select dm.mealType, b.status, count(b), sum(b.priceAtBooking) from Booking b join b.dailyMenu dm "
                + "where dm.menuDate between :from and :to and b.status in :st group by dm.mealType, b.status",
                "from", from, "to", to, "st", List.of(BookingStatus.CONSUMED, BookingStatus.NO_SHOW))) {
            MealType m = (MealType) r[0];
            long[] a = counts.computeIfAbsent(m, k -> new long[2]);
            a[0] += ((Number) r[2]).longValue();
            if (r[1] == BookingStatus.NO_SHOW) {
                a[1] += ((Number) r[2]).longValue();
                lost.merge(m, money(r[3]), BigDecimal::add);
            }
        }
        long resolved = 0, noShow = 0;
        BigDecimal lostTotal = BigDecimal.ZERO;
        List<MealWastage> byMeal = new ArrayList<>();
        for (MealType m : MealType.values()) {
            long[] a = counts.getOrDefault(m, new long[2]);
            BigDecimal l = lost.getOrDefault(m, BigDecimal.ZERO);
            byMeal.add(new MealWastage(m, a[0], a[1], a[0] == 0 ? 0 : round(100.0 * a[1] / a[0]), l));
            resolved += a[0];
            noShow += a[1];
            lostTotal = lostTotal.add(l);
        }
        return new Wastage(resolved, noShow, resolved == 0 ? 0 : round(100.0 * noShow / resolved), lostTotal, byMeal);
    }

    public List<RatingPoint> ratingsTrend(int days) {
        int n = clampDays(days);
        LocalDate to = LocalDate.now(clock), from = to.minusDays(n - 1L);
        Map<LocalDate, Object[]> byDay = new HashMap<>();
        for (Object[] r : rows("select dm.menuDate, avg(f.rating), count(f) from Feedback f join f.dailyMenu dm "
                + "where dm.menuDate between :from and :to group by dm.menuDate", "from", from, "to", to)) {
            byDay.put((LocalDate) r[0], r);
        }
        List<RatingPoint> out = new ArrayList<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            Object[] r = byDay.get(d);
            out.add(r == null ? new RatingPoint(d, null, 0) : new RatingPoint(d, round(((Number) r[1]).doubleValue()), ((Number) r[2]).longValue()));
        }
        return out;
    }

    /** A dish's score is the average rating of the meals it was served in (students rate meals, not single dishes). */
    public List<DishScore> topDishes(int days, int limit, boolean best, int minRatings) {
        int n = clampDays(days);
        LocalDate to = LocalDate.now(clock), from = to.minusDays(n - 1L);
        Query q = em.createQuery("select i.id, i.name, avg(f.rating), count(f) from Feedback f join f.dailyMenu dm join dm.items i "
                + "where dm.menuDate between :from and :to group by i.id, i.name having count(f) >= :min "
                + "order by avg(f.rating) " + (best ? "desc" : "asc") + ", count(f) desc, i.name", Object[].class);
        q.setParameter("from", from).setParameter("to", to).setParameter("min", (long) Math.max(1, minRatings));
        q.setMaxResults(Math.max(1, Math.min(limit, 50)));
        @SuppressWarnings("unchecked")
        List<Object[]> res = q.getResultList();
        return res.stream().map(r -> new DishScore((Long) r[0], (String) r[1], round(((Number) r[2]).doubleValue()), ((Number) r[3]).longValue())).toList();
    }

    public List<RevenuePoint> revenue(int months) {
        int n = Math.max(1, Math.min(months, 24));
        YearMonth thisMonth = YearMonth.from(LocalDate.now(clock));
        LocalDate from = thisMonth.minusMonths(n - 1L).atDay(1), to = thisMonth.atEndOfMonth();
        Map<LocalDate, BigDecimal[]> byMonth = new HashMap<>();   // [billed, collected, outstanding, bills, paidBills]
        for (Object[] r : rows("select b.billMonth, b.status, count(b), sum(b.total) from Bill b "
                + "where b.billMonth between :from and :to group by b.billMonth, b.status", "from", from, "to", to)) {
            BigDecimal[] a = byMonth.computeIfAbsent((LocalDate) r[0], k -> new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            BigDecimal count = BigDecimal.valueOf(((Number) r[2]).longValue()), sum = money(r[3]);
            a[0] = a[0].add(sum);
            a[3] = a[3].add(count);
            if (r[1] == BillStatus.PAID) { a[1] = a[1].add(sum); a[4] = a[4].add(count); } else { a[2] = a[2].add(sum); }
        }
        List<RevenuePoint> out = new ArrayList<>();
        for (int i = n - 1; i >= 0; i--) {
            YearMonth m = thisMonth.minusMonths(i);
            BigDecimal[] a = byMonth.getOrDefault(m.atDay(1), new BigDecimal[] {BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO});
            out.add(new RevenuePoint(m.toString(), a[0], a[1], a[2], a[3].longValue(), a[4].longValue()));
        }
        return out;
    }

    // ------------------------------------------------------------------ helpers

    /** [booked(non-cancelled), served, noShow, cancelled] per meal over the window. */
    private Map<MealType, long[]> statusCounts(int days) {
        int n = clampDays(days);
        LocalDate to = LocalDate.now(clock), from = to.minusDays(n - 1L);
        Map<MealType, long[]> out = new EnumMap<>(MealType.class);
        for (MealType m : MealType.values()) out.put(m, new long[4]);
        for (Object[] r : rows("select dm.mealType, b.status, count(b) from Booking b join b.dailyMenu dm "
                + "where dm.menuDate between :from and :to group by dm.mealType, b.status", "from", from, "to", to)) {
            long[] a = out.get((MealType) r[0]);
            long c = ((Number) r[2]).longValue();
            switch ((BookingStatus) r[1]) {
                case CONSUMED -> { a[0] += c; a[1] += c; }
                case NO_SHOW -> { a[0] += c; a[2] += c; }
                case BOOKED -> a[0] += c;
                case CANCELLED -> a[3] += c;
            }
        }
        return out;
    }

    @SuppressWarnings("unchecked")
    private List<Object[]> rows(String jpql, Object... params) {
        Query q = em.createQuery(jpql);
        for (int i = 0; i < params.length; i += 2) q.setParameter((String) params[i], params[i + 1]);
        return q.getResultList();
    }

    private Object one(String jpql, Object... params) {
        Query q = em.createQuery(jpql);
        for (int i = 0; i < params.length; i += 2) q.setParameter((String) params[i], params[i + 1]);
        return q.getSingleResult();
    }

    private long count(String jpql, Object... params) {
        return ((Number) one(jpql, params)).longValue();
    }

    private static BigDecimal money(Object v) {
        return v == null ? BigDecimal.ZERO : ((BigDecimal) v).setScale(2, RoundingMode.HALF_UP);
    }

    private static int clampDays(int days) {
        return Math.max(1, Math.min(days, 120));
    }

    private static double round(double v) {
        return Math.round(v * 100.0) / 100.0;
    }

}
