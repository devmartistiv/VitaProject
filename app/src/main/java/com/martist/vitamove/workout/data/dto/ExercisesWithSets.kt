package com.martist.vitamove.workout.data.dto

import androidx.room.Embedded
import androidx.room.Relation
import com.martist.vitamove.set.ExerciseSetEntity
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity

data class ExercisesWithSets(
    @Embedded
    val exerciseEntity: WorkoutExerciseEntity,
    @Relation(
        entity = ExerciseSetEntity::class,
        parentColumn = "id",
        entityColumn = "workout_exercise_id"
    )
    val sets: List<ExerciseSetEntity>
)
