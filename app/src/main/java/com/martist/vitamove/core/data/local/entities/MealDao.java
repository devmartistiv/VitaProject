package com.martist.vitamove.core.data.local.entities;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface MealDao {
    @Query("SELECT * FROM meals WHERE date = :date AND user_id = :userId")
    List<DayMeal> getMealsForDate(String date, String userId);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(DayMeal meal);

    @Query("DELETE FROM meals WHERE date < :cutoffDate AND user_id = :userId")
    void deleteOlderThan(String cutoffDate, String userId);

    @Update
    void update(DayMeal meal);

    @Query("SELECT * FROM meals WHERE date = :date AND meal_type = :mealType AND user_id = :userId LIMIT 1")
    DayMeal getMealByDateAndType(String date, String mealType, String userId);

    @Query("DELETE FROM meals WHERE user_id = :userId")
    void deleteAllMealsForUser(String userId);


    @Query("SELECT * FROM meals WHERE date = :date AND meal_type = :mealType AND user_id = :userId LIMIT 1")
    DayMeal getMealForDateAndType(String date, String mealType, String userId);


    @Query("SELECT * FROM meals WHERE user_id = :userId")
    List<DayMeal> getAllMeals(String userId);
} 