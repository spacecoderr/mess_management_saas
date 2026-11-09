package com.thali.announcement;

import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {

    @EntityGraph(attributePaths = "author")
    List<Announcement> findTop50ByOrderByPinnedDescCreatedAtDesc();
}
