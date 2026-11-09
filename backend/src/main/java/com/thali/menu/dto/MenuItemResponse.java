package com.thali.menu.dto;

import com.thali.menu.MealCategory;
import com.thali.menu.MenuItem;

public record MenuItemResponse(Long id, String name, String description, MealCategory category, boolean veg,
                               String imageUrl, boolean active) {
    public static MenuItemResponse of(MenuItem m) {
        return new MenuItemResponse(m.getId(), m.getName(), m.getDescription(), m.getCategory(), m.isVeg(), m.getImageUrl(), m.isActive());
    }
}
