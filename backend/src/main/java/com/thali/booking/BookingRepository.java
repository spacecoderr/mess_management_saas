package com.thali.booking;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thali.menu.MealType;

public interface BookingRepository extends JpaRepository<Booking, Long> {

    long countByDailyMenuIdAndStatus(Long dailyMenuId, BookingStatus status);

    Optional<Booking> findByStudentIdAndDailyMenuId(Long studentId, Long dailyMenuId);

    @Query("""
            select b from Booking b join fetch b.dailyMenu dm
            where b.student.id = :sid and dm.menuDate between :from and :to
            order by dm.menuDate, dm.mealType
            """)
    List<Booking> findForStudent(@Param("sid") Long studentId, @Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select b from Booking b join fetch b.dailyMenu join fetch b.student where b.id = :id")
    Optional<Booking> findDetailed(@Param("id") Long id);

    @Query("select b from Booking b join fetch b.dailyMenu join fetch b.student where b.qrToken = :token")
    Optional<Booking> findByQr(@Param("token") UUID token);

    /** `q` is lower-cased and %-wrapped by the caller ("%" = no filter). */
    @Query("""
            select b from Booking b join fetch b.dailyMenu dm join fetch b.student s
            where dm.menuDate = :date and dm.mealType in :types and b.status in :statuses
              and (:q = '%' or lower(s.name) like :q
                   or exists (select 1 from StudentProfile sp where sp.userId = s.id and lower(sp.rollNo) like :q))
            order by s.name
            """)
    List<Booking> findForStaff(@Param("date") LocalDate date, @Param("types") Collection<MealType> types,
                               @Param("statuses") Collection<BookingStatus> statuses, @Param("q") String q);

    @Query("select b.status, count(b) from Booking b where b.dailyMenu.id = :menuId group by b.status")
    List<Object[]> countByStatus(@Param("menuId") Long menuId);

    /** Meals the student actually ate but hasn't rated yet. */
    @Query("""
            select b from Booking b join fetch b.dailyMenu dm
            where b.student.id = :sid and b.status = com.thali.booking.BookingStatus.CONSUMED
              and not exists (select 1 from Feedback f where f.student.id = b.student.id and f.dailyMenu.id = dm.id)
            order by dm.menuDate desc, dm.mealType
            """)
    List<Booking> findUnrated(@Param("sid") Long studentId);

    /** One row per student: [studentId, mealCount, totalPrice] for the given statuses in a date range. */
    @Query("""
            select b.student.id, count(b), sum(b.priceAtBooking) from Booking b join b.dailyMenu dm
            where dm.menuDate between :from and :to and b.status in :statuses
            group by b.student.id
            """)
    List<Object[]> billingTotals(@Param("from") LocalDate from, @Param("to") LocalDate to,
                                 @Param("statuses") Collection<BookingStatus> statuses);

    @Query("""
            select b from Booking b join fetch b.dailyMenu dm
            where b.student.id = :sid and dm.menuDate between :from and :to and b.status in :statuses
            order by dm.menuDate, dm.mealType
            """)
    List<Booking> findBillable(@Param("sid") Long studentId, @Param("from") LocalDate from, @Param("to") LocalDate to,
                               @Param("statuses") Collection<BookingStatus> statuses);

    /** Still-BOOKED bookings whose meal date has arrived or passed (candidates for no-show). */
    @Query("select b from Booking b join fetch b.dailyMenu dm where b.status = com.thali.booking.BookingStatus.BOOKED and dm.menuDate <= :today")
    List<Booking> findOpenUpTo(@Param("today") LocalDate today);
}
