package com.martist.vitamove.workout.data.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity

data class WorkoutWithExercises(
    @Embedded
    val workout: UserWorkoutEntity,
    @Relation(
        entity = WorkoutExerciseEntity::class,
        parentColumn = "id",
        entityColumn = "workout_id"
    )
    val exercises: List<ExercisesWithSets>
)
