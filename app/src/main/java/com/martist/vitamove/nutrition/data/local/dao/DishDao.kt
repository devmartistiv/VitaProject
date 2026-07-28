package com.martist.vitamove.nutrition.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.martist.vitamove.nutrition.data.local.entities.DishEntity
import com.martist.vitamove.nutrition.data.local.entities.DishIngredientEntity

@Dao
interface DishDao {
    @Insert
    fun insertDish(dish: DishEntity)

    @Update
    fun updateDish(dish: DishEntity)

    @Delete
    fun deleteDish(dish: DishEntity)

    @Query("DELETE  FROM dishes WHERE id = :id")
    fun deleteDishById(id: String)

    @get:Query("SELECT * FROM dishes ORDER BY updatedAt DESC")
    val allDishes: List<DishEntity>

    @Query("SELECT * FROM dishes WHERE id = :dishId")
    fun getDishById(dishId: String): DishEntity

    @Query("SELECT * FROM dishes WHERE name LIKE '%' || :name || '%' ORDER BY updatedAt DESC")
    fun searchDishesByName(name: String): List<DishEntity>


    @Insert
    fun insertIngredient(ingredient: DishIngredientEntity)

    @Insert
    fun insertIngredients(ingredients: List<DishIngredientEntity>)

    @Delete
    fun deleteIngredient(ingredient: DishIngredientEntity)

    @Query("DELETE FROM dish_ingredients WHERE dishId = :dishId")
    fun deleteAllIngredientsForDish(dishId: String)

    @Query("SELECT * FROM dish_ingredients WHERE dishId = :dishId ORDER BY createdAt ASC")
    fun getIngredientsForDish(dishId: String): List<DishIngredientEntity>


    @Transaction
    @Query("SELECT * FROM dishes ORDER BY updatedAt DESC")
    fun getAllDishesWithIngredients(): List<DishWithIngredients>

    @Transaction
    @Query("SELECT * FROM dishes WHERE id = :dishId")
    fun getDishWithIngredients(dishId: String): DishWithIngredients


    @Transaction
    fun createDishWithIngredients(
        dish: DishEntity,
        ingredients: List<DishIngredientEntity>
    ) {
        insertDish(dish)


        for (ingredient in ingredients) {
            ingredient.setDishId(dish.id)
        }

        insertIngredients(ingredients)
    }

    @Transaction
    fun updateDishWithIngredients(
        dish: DishEntity,
        ingredients: List<DishIngredientEntity>
    ) {
        updateDish(dish)


        deleteAllIngredientsForDish(dish.id)


        for (ingredient in ingredients) {
            ingredient.setDishId(dish.id)
        }
        insertIngredients(ingredients)
    }

    @Transaction
    fun deleteDishWithIngredients(id: String) {
        deleteAllIngredientsForDish(id)
        deleteDishById(id)
    }

    @get:Query("SELECT COUNT(*) FROM dishes")
    val dishCount: Int

    @Query("SELECT COUNT(*) FROM dish_ingredients WHERE dishId = :dishId")
    fun getIngredientCountForDish(dishId: String): Int
}