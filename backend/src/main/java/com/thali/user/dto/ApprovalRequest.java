package com.thali.user.dto;

import com.thali.user.ApprovalStatus;

import jakarta.validation.constraints.NotNull;

public record ApprovalRequest(@NotNull ApprovalStatus status) {}
