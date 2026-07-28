package com.martist.vitamove.nutrition.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.martist.vitamove.nutrition.data.local.entities.RecentFoodEntity
import java.util.Date


@Dao
interface RecentFoodDao {
    @get:Query("SELECT * FROM recent_foods ORDER BY last_used_at DESC LIMIT 50")
    val allRecentFoods: List<RecentFoodEntity>


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun addToRecents(recentFoodEntity: RecentFoodEntity)


    @Delete
    fun removeFromRecents(recentFoodEntity: RecentFoodEntity)


    @Query("DELETE FROM recent_foods WHERE food_id = :foodId")
    fun removeFromRecentsByFoodId(foodId: String)


    @Query("SELECT COUNT(*) > 0 FROM recent_foods WHERE food_id = :foodId")
    fun isFoodInRecents(foodId: String): Boolean


    @Query("SELECT * FROM recent_foods WHERE food_id = :foodId")
    fun getRecentFoodById(foodId: String): RecentFoodEntity

    @get:Query("SELECT COUNT(*) FROM recent_foods")
    val recentFoodsCount: Int


    @Query("DELETE FROM recent_foods")
    fun clearAllRecents()

    @Query("UPDATE recent_foods SET last_used_at = :lastUsedAt WHERE food_id = :foodId")
    fun updateLastUsedTime(foodId: String, lastUsedAt: Date)


    @Query("SELECT * FROM recent_foods WHERE food_id = :foodId AND quantity = :quantity AND portionName = :portionName LIMIT 1")
    fun getExistingRecentFood(
        foodId: String,
        quantity: Float,
        portionName: String
    ): RecentFoodEntity


    @Query("UPDATE recent_foods SET last_used_at = :lastUsedAt WHERE id = :id")
    fun updateLastUsedTimeById(id: String, lastUsedAt: Date)

    @Query("UPDATE recent_foods SET quantity = :quantity,portionName = :portionName WHERE id = :id")
    fun updatePortion(id: String, quantity: Float, portionName: String)


    @Query(
        ("DELETE FROM recent_foods WHERE rowid NOT IN " +
                "(SELECT MAX(rowid) FROM recent_foods " +
                "GROUP BY food_id, quantity, portionName)")
    )
    fun removeDuplicates()
}