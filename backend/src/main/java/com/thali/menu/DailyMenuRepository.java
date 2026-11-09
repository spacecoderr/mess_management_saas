package com.thali.menu;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DailyMenuRepository extends JpaRepository<DailyMenu, Long> {

    @Query("""
            select distinct dm from DailyMenu dm left join fetch dm.items
            where dm.menuDate between :from and :to and (dm.published = true or :includeDrafts = true)
            order by dm.menuDate, dm.mealType
            """)
    List<DailyMenu> findRange(@Param("from") LocalDate from, @Param("to") LocalDate to,
                              @Param("includeDrafts") boolean includeDrafts);

    @Query("select dm from DailyMenu dm left join fetch dm.items where dm.menuDate = :date and dm.mealType = :type")
    Optional<DailyMenu> findByDateAndType(@Param("date") LocalDate date, @Param("type") MealType type);

    @Query("select dm from DailyMenu dm left join fetch dm.items where dm.id = :id")
    Optional<DailyMenu> findWithItems(@Param("id") Long id);
}
