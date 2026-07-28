package com.martist.vitamove.nutrition.data.local.entities

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

@Entity(tableName = "recent_foods")
data class RecentFoodEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    var id: String,


    @ColumnInfo(name = "food_id")
    var foodId: String,

    @ColumnInfo(name = "food_name")
    var foodName: String,

    @ColumnInfo(name = "quantity")
    var quantity: Float = 0f,

    @ColumnInfo(name = "portionName")
    var portionName: String,

    @ColumnInfo(name = "created_at")
    var createdAt: Date = Date(),

    @ColumnInfo(name = "last_used_at")
    var last_used_at: Date = Date(),
)
