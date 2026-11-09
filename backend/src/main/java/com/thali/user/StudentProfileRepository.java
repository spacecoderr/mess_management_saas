package com.thali.user;

import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface StudentProfileRepository extends JpaRepository<StudentProfile, Long> {

    boolean existsByRollNo(String rollNo);

    @EntityGraph(attributePaths = "user")
    @Query("""
            select sp from StudentProfile sp
            where sp.approvalStatus in :statuses
              and (lower(sp.user.name) like :q or lower(sp.rollNo) like :q or lower(sp.user.email) like :q)
            """)
    Page<StudentProfile> search(@Param("statuses") Collection<ApprovalStatus> statuses, @Param("q") String q, Pageable pageable);

    @EntityGraph(attributePaths = "user")
    @Query("select sp from StudentProfile sp where sp.approvalStatus = com.thali.user.ApprovalStatus.APPROVED and sp.user.active = true")
    java.util.List<StudentProfile> findAllApprovedActive();
}
