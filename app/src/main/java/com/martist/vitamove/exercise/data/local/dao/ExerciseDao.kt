package com.martist.vitamove.exercise.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.martist.vitamove.exercise.data.local.entities.ExerciseEntity

@Dao
interface ExerciseDao {
    @Query("SELECT * FROM exercises")
    fun getAllExercises(): List<ExerciseEntity>

    @Query("SELECT * FROM exercises WHERE id = :exerciseId")
    fun getExerciseById(exerciseId: String): ExerciseEntity


    @Query("SELECT * FROM exercises WHERE id IN (:exerciseIds)")
    fun getExercisesByIds(exerciseIds: List<String>): List<ExerciseEntity>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insertExercise(exercise: ExerciseEntity)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insertExercises(exercises: List<ExerciseEntity>)

    @Query("DELETE FROM exercises")
    fun deleteAllExercises()

    @Transaction
    fun updateExercises(exercises: List<ExerciseEntity>) {
        deleteAllExercises()
        insertExercises(exercises)
    }

    @get:Query("SELECT COUNT(*) FROM exercises")
    val exerciseCount: Int
}