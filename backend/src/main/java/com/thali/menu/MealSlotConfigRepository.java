package com.thali.menu;

import org.springframework.data.jpa.repository.JpaRepository;

public interface MealSlotConfigRepository extends JpaRepository<MealSlotConfig, MealType> {
}
