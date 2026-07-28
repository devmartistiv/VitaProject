package com.martist.vitamove.nutrition.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "favorite_foods")
data class FavoriteFoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "food_id")
    var foodId: String,

    @ColumnInfo(name = "food_name")
    var foodName: String,
    @ColumnInfo(name = "food_category")
    var foodCategory: String,

    @ColumnInfo(name = "food_subcategory")
    var foodSubcategory: String,

    @ColumnInfo(name = "calories")
    var calories: Float = 0f,

    @ColumnInfo(name = "proteins")
    var proteins: Float = 0f,

    @ColumnInfo(name = "fats")
    var fats: Float = 0f,

    @ColumnInfo(name = "carbs")
    var carbs: Float = 0f,

    @ColumnInfo(name = "created_at")
    var createdAt: Date = Date()

)
