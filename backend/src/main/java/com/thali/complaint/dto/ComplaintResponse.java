package com.thali.complaint.dto;

import java.time.Instant;
import java.util.List;

import com.thali.complaint.Complaint;
import com.thali.complaint.ComplaintCategory;
import com.thali.complaint.ComplaintPriority;
import com.thali.complaint.ComplaintStatus;

/** `updates` (the timeline) is only filled on the detail endpoint. */
public record ComplaintResponse(Long id, ComplaintCategory category, String title, String description, ComplaintPriority priority,
                                ComplaintStatus status, Long studentId, String studentName, String rollNo,
                                Long assignedToId, String assignedToName, Instant createdAt, Instant updatedAt,
                                List<UpdateEntryResponse> updates) {

    public static ComplaintResponse of(Complaint c, String rollNo, List<UpdateEntryResponse> updates) {
        var a = c.getAssignedTo();
        return new ComplaintResponse(c.getId(), c.getCategory(), c.getTitle(), c.getDescription(), c.getPriority(), c.getStatus(),
                c.getStudent().getId(), c.getStudent().getName(), rollNo, a == null ? null : a.getId(),
                a == null ? null : a.getName(), c.getCreatedAt(), c.getUpdatedAt(), updates);
    }
}
