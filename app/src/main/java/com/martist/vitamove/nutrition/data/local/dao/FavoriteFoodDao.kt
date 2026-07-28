package com.martist.vitamove.nutrition.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.martist.vitamove.nutrition.data.local.entities.FavoriteFoodEntity

@Dao
interface FavoriteFoodDao {
    @get:Query("SELECT * FROM favorite_foods ORDER BY created_at DESC")
    val allFavoriteFoods: List<FavoriteFoodEntity>


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun addToFavorites(favoriteFoodEntity: FavoriteFoodEntity)


    @Delete
    fun removeFromFavorites(favoriteFoodEntity: FavoriteFoodEntity)


    @Query("DELETE FROM favorite_foods WHERE food_id = :foodId")
    fun removeFromFavoritesByFoodId(foodId: String)


    @Query("SELECT COUNT(*) > 0 FROM favorite_foods WHERE food_id = :foodId")
    fun isFoodInFavorites(foodId: String): Boolean


    @Query("SELECT * FROM favorite_foods WHERE food_id = :foodId")
    fun getFavoriteFoodById(foodId: String): FavoriteFoodEntity

    @get:Query("SELECT COUNT(*) FROM favorite_foods")
    val favoriteFoodsCount: Int


    @Query("DELETE FROM favorite_foods")
    fun clearAllFavorites()
}