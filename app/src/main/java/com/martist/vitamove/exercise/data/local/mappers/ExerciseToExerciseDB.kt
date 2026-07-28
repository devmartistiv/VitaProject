package com.martist.vitamove.exercise.data.local.mappers

import com.martist.vitamove.exercise.data.local.entities.ExerciseEntity
import com.martist.vitamove.exercise.ui.model.Exercise

class ExerciseToExerciseDB : (Exercise) -> ExerciseEntity {

    override fun invoke(exercise: Exercise): ExerciseEntity {
        return ExerciseEntity(
            exercise.id,
            exercise.name,
            exercise.description,
            exercise.difficulty,
            exercise.exerciseType ?: "другое",
            exercise.met,
            exercise.categories,
            exercise.muscleGroups,
            exercise.muscleGroupRussianNames,
            exercise.equipmentRequired,
            exercise.instructions,
            exercise.category
        )
    }
}
