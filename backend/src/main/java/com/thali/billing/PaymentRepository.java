package com.thali.billing;

import java.time.Instant;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PaymentRepository extends JpaRepository<Payment, Long> {

    @Query("select p from Payment p join fetch p.bill b join fetch b.student left join fetch p.recordedBy where p.id = :id")
    Optional<Payment> findDetailed(@Param("id") Long id);

    List<Payment> findByBillIdAndStatus(Long billId, PaymentStatus status);

    @Query("select p from Payment p join fetch p.bill b join fetch b.student left join fetch p.recordedBy where b.student.id = :sid order by p.createdAt desc")
    List<Payment> findMine(@Param("sid") Long studentId);

    @Query(value = """
            select p from Payment p join fetch p.bill b join fetch b.student left join fetch p.recordedBy
            where p.createdAt >= :from and p.createdAt < :to and p.status in :statuses
            """,
            countQuery = "select count(p) from Payment p where p.createdAt >= :from and p.createdAt < :to and p.status in :statuses")
    Page<Payment> search(@Param("from") Instant from, @Param("to") Instant to,
                         @Param("statuses") Collection<PaymentStatus> statuses, Pageable pageable);
}
