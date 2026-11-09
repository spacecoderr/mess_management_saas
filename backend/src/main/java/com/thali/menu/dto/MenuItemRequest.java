package com.thali.menu.dto;

import com.thali.menu.MealCategory;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record MenuItemRequest(
        @NotBlank @Size(max = 120) String name,
        @Size(max = 500) String description,
        @NotNull MealCategory category,
        @NotNull Boolean veg,
        @Size(max = 500) String imageUrl,
        Boolean active) {}
