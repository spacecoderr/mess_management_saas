package com.thali.menu.dto;

import java.util.List;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Replaces the dish list for one date + meal. `publish` (optional) publishes/unpublishes in the same call. */
public record SetMenuRequest(@NotNull @Size(max = 30) List<Long> itemIds, Boolean publish) {}
