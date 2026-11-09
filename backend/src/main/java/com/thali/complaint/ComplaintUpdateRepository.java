package com.thali.complaint;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplaintUpdateRepository extends JpaRepository<ComplaintUpdate, Long> {

    @EntityGraph(attributePaths = "author")
    List<ComplaintUpdate> findByComplaintIdOrderByCreatedAtAsc(Long complaintId);
}
