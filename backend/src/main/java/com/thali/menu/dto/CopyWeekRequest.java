package com.thali.menu.dto;

import java.time.LocalDate;

import jakarta.validation.constraints.NotNull;

/** Copies the 7 days starting at fromWeekStart onto the 7 days starting at toWeekStart (as unpublished drafts). */
public record CopyWeekRequest(@NotNull LocalDate fromWeekStart, @NotNull LocalDate toWeekStart, Boolean overwrite) {}
