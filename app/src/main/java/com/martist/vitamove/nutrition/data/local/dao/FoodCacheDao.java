package com.martist.vitamove.nutrition.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import com.martist.vitamove.nutrition.data.local.entities.FoodCacheEntity;

import java.util.List;


@Dao
public interface FoodCacheDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<FoodCacheEntity> foods);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(FoodCacheEntity food);


    @Query("SELECT * FROM food_cache WHERE " +
            "name LIKE :query OR category LIKE :query OR subcategory LIKE :query " +
            "ORDER BY popularity DESC " +
            "LIMIT 100")
    List<FoodCacheEntity> searchFoods(String query);


    @Query("SELECT * FROM food_cache ORDER BY popularity DESC")
    List<FoodCacheEntity> getAllFoods();


    @Query("SELECT * FROM food_cache WHERE id = :id LIMIT 1")
    FoodCacheEntity getFoodById(String id);


    @Query("SELECT MAX(updated_at) FROM food_cache")
    String getLastSyncTimestamp();


    @Query("SELECT COUNT(*) FROM food_cache")
    int getFoodCount();


    @Query("DELETE FROM food_cache")
    void clearAll();


    @Query("SELECT * FROM food_cache ORDER BY popularity DESC LIMIT :limit")
    List<FoodCacheEntity> getPopularFoods(int limit);


    @Query("SELECT DISTINCT category FROM food_cache ORDER BY category")
    List<String> getAllCategories();


    @Query("SELECT DISTINCT subcategory FROM food_cache WHERE category = :category ORDER BY subcategory")
    List<String> getSubcategoriesForCategory(String category);


    @Query("SELECT * FROM food_cache WHERE name = :name LIMIT 1")
    FoodCacheEntity getFoodByName(String name);
}





