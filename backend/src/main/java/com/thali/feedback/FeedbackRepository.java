package com.thali.feedback;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.thali.menu.MealType;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    boolean existsByStudentIdAndDailyMenuId(Long studentId, Long dailyMenuId);

    @Query("select f from Feedback f join fetch f.dailyMenu where f.student.id = :sid order by f.createdAt desc")
    List<Feedback> findMine(@Param("sid") Long studentId);

    @Query(value = """
            select f from Feedback f join fetch f.dailyMenu dm join fetch f.student
            where dm.menuDate between :from and :to and dm.mealType in :types
            """,
            countQuery = "select count(f) from Feedback f join f.dailyMenu dm where dm.menuDate between :from and :to and dm.mealType in :types")
    Page<Feedback> search(@Param("from") LocalDate from, @Param("to") LocalDate to,
                          @Param("types") Collection<MealType> types, Pageable pageable);

    @Query("select dm.mealType, avg(f.rating), count(f) from Feedback f join f.dailyMenu dm where dm.menuDate between :from and :to group by dm.mealType")
    List<Object[]> averageByMeal(@Param("from") LocalDate from, @Param("to") LocalDate to);

    @Query("select f.rating, count(f) from Feedback f join f.dailyMenu dm where dm.menuDate between :from and :to group by f.rating")
    List<Object[]> distribution(@Param("from") LocalDate from, @Param("to") LocalDate to);
}
