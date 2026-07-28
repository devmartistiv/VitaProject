package com.martist.vitamove.exercise.data.local.entities

import androidx.room.Entity
import androidx.room.PrimaryKey
import androidx.room.TypeConverters
import com.martist.vitamove.core.data.local.converters.ListConverter

@Entity(tableName = "exercises")
data class ExerciseEntity(
    @PrimaryKey
    var id: String,
    var name: String,
    var description: String,
    var difficulty: String,
    var exerciseType: String,
    var met: Float = 0f,

    @TypeConverters(ListConverter::class)
    var categories: MutableList<String?>,

    @TypeConverters(ListConverter::class)
    var muscleGroups: MutableList<String?>,

    @TypeConverters(ListConverter::class)
    var muscleGroupRussianNames: MutableList<String?>,

    @TypeConverters(ListConverter::class)
    var equipmentRequired: MutableList<String?>,


    var instructions: String,
    val category: String = categories[0] ?: ""

)