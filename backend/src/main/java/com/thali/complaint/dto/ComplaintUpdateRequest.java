package com.thali.complaint.dto;

import com.thali.complaint.ComplaintStatus;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** newStatus is optional and only staff/admin may set it. */
public record ComplaintUpdateRequest(@NotBlank @Size(max = 1000) String message, ComplaintStatus newStatus) {}
