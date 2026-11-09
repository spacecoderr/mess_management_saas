package com.thali.analytics;

import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.thali.billing.Bill;
import com.thali.billing.BillRepository;
import com.thali.booking.BookingStatus;
import com.thali.common.exception.ApiException;
import com.thali.user.StudentProfile;
import com.thali.user.StudentProfileRepository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;

@Service
@Transactional(readOnly = true)
public class ReportService {

    @PersistenceContext
    private EntityManager em;

    private final BillRepository bills;
    private final StudentProfileRepository profiles;

    public ReportService(BillRepository bills, StudentProfileRepository profiles) {
        this.bills = bills;
        this.profiles = profiles;
    }

    public byte[] billsCsv(YearMonth month) {
        List<Bill> list = bills.findForMonth(month.atDay(1));
        Map<Long, String> rolls = profiles.findAllById(list.stream().map(b -> b.getStudent().getId()).toList()).stream()
                .collect(Collectors.toMap(StudentProfile::getUserId, StudentProfile::getRollNo));
        Csv csv = new Csv().row("roll_no", "name", "email", "month", "meals", "meal_charges", "fixed_charges", "total", "status", "due_date");
        for (Bill b : list) {
            csv.row(rolls.get(b.getStudent().getId()), b.getStudent().getName(), b.getStudent().getEmail(), month, b.getMealCount(),
                    b.getMealCharges(), b.getFixedCharges(), b.getTotal(), b.getStatus(), b.getDueDate());
        }
        return csv.bytes();
    }

    @SuppressWarnings("unchecked")
    public byte[] attendanceCsv(LocalDate from, LocalDate to) {
        if (to.isBefore(from) || to.isAfter(from.plusDays(92))) {
            throw ApiException.badRequest("INVALID_RANGE", "Choose a range of at most 92 days.");
        }
        List<Object[]> rows = em.createQuery("""
                select dm.menuDate, dm.mealType, sp.rollNo, s.name, b.status, a.method, a.markedAt, m.name
                from Booking b join b.dailyMenu dm join b.student s
                left join StudentProfile sp on sp.userId = s.id
                left join Attendance a on a.booking.id = b.id
                left join a.markedBy m
                where dm.menuDate between :from and :to and b.status in :st
                order by dm.menuDate, dm.mealType, s.name
                """, Object[].class)
                .setParameter("from", from).setParameter("to", to)
                .setParameter("st", List.of(BookingStatus.CONSUMED, BookingStatus.NO_SHOW)).getResultList();
        Csv csv = new Csv().row("date", "meal", "roll_no", "name", "status", "method", "marked_at", "marked_by");
        rows.forEach(r -> csv.row(r));
        return csv.bytes();
    }
}
