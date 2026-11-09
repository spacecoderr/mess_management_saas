package com.thali.complaint.dto;

import com.thali.complaint.ComplaintCategory;
import com.thali.complaint.ComplaintPriority;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateComplaintRequest(
        @NotNull ComplaintCategory category,
        @NotBlank @Size(max = 150) String title,
        @NotBlank @Size(max = 2000) String description,
        ComplaintPriority priority) {}
