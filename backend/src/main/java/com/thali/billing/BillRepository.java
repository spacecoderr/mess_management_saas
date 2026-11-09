package com.thali.billing;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface BillRepository extends JpaRepository<Bill, Long> {

    Optional<Bill> findByStudentIdAndBillMonth(Long studentId, LocalDate billMonth);

    @Query("select b from Bill b join fetch b.student where b.student.id = :sid order by b.billMonth desc")
    List<Bill> findMine(@Param("sid") Long studentId);

    @Query("select b from Bill b join fetch b.student where b.id = :id")
    Optional<Bill> findDetailed(@Param("id") Long id);

    /** Month filter is a first-of-month range (never a nullable parameter: Postgres can't infer the type of `? is null`). */
    @Query(value = """
            select b from Bill b join fetch b.student
            where b.billMonth between :from and :to and b.status in :statuses
            """,
            countQuery = "select count(b) from Bill b where b.billMonth between :from and :to and b.status in :statuses")
    Page<Bill> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                      @Param("statuses") Collection<BillStatus> statuses, Pageable pageable);

    @Query("select b from Bill b join fetch b.student where b.billMonth = :month order by b.student.name")
    List<Bill> findForMonth(@Param("month") LocalDate month);

    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Bill b set b.status = com.thali.billing.BillStatus.OVERDUE where b.status = com.thali.billing.BillStatus.UNPAID and b.dueDate < :today")
    int markOverdue(@Param("today") LocalDate today);
}
