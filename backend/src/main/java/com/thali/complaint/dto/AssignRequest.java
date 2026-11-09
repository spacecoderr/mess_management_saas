package com.thali.complaint.dto;

import jakarta.validation.constraints.NotNull;

public record AssignRequest(@NotNull Long staffId) {}
