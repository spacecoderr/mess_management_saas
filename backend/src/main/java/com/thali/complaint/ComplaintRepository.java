package com.thali.complaint;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ComplaintRepository extends JpaRepository<Complaint, Long> {

    @Query("select c from Complaint c join fetch c.student left join fetch c.assignedTo where c.student.id = :sid order by c.createdAt desc")
    List<Complaint> findMine(@Param("sid") Long studentId);

    @Query("select c from Complaint c join fetch c.student left join fetch c.assignedTo where c.id = :id")
    Optional<Complaint> findDetailed(@Param("id") Long id);

    /** assignee = 0 means "anyone". */
    @Query(value = """
            select c from Complaint c join fetch c.student left join fetch c.assignedTo
            where c.status in :statuses and c.category in :categories and (:assignee = 0L or c.assignedTo.id = :assignee)
            """,
            countQuery = """
            select count(c) from Complaint c
            where c.status in :statuses and c.category in :categories and (:assignee = 0L or c.assignedTo.id = :assignee)
            """)
    Page<Complaint> search(@Param("statuses") Collection<ComplaintStatus> statuses,
                           @Param("categories") Collection<ComplaintCategory> categories,
                           @Param("assignee") Long assignee, Pageable pageable);
}
