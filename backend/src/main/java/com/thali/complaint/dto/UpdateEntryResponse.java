package com.thali.complaint.dto;

import java.time.Instant;

import com.thali.complaint.ComplaintStatus;
import com.thali.complaint.ComplaintUpdate;
import com.thali.user.Role;

public record UpdateEntryResponse(Long id, String authorName, Role authorRole, String message, ComplaintStatus newStatus, Instant createdAt) {
    public static UpdateEntryResponse of(ComplaintUpdate u) {
        return new UpdateEntryResponse(u.getId(), u.getAuthor().getName(), u.getAuthor().getRole(), u.getMessage(), u.getNewStatus(), u.getCreatedAt());
    }
}
