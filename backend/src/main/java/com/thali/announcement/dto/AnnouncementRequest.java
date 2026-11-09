package com.thali.announcement.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnnouncementRequest(@NotBlank @Size(max = 150) String title, @NotBlank @Size(max = 2000) String body, Boolean pinned) {}
