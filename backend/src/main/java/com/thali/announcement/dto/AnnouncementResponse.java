package com.thali.announcement.dto;

import java.time.Instant;

import com.thali.announcement.Announcement;

public record AnnouncementResponse(Long id, String title, String body, boolean pinned, String author, Instant createdAt) {
    public static AnnouncementResponse of(Announcement a) {
        return new AnnouncementResponse(a.getId(), a.getTitle(), a.getBody(), a.isPinned(), a.getAuthor().getName(), a.getCreatedAt());
    }
}
