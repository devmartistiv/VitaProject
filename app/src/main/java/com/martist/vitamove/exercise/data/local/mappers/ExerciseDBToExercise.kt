package com.martist.vitamove.exercise.data.local.mappers

import com.martist.vitamove.exercise.data.local.entities.ExerciseEntity
import com.martist.vitamove.exercise.ui.model.Exercise

class ExerciseDBToExercise : (ExerciseEntity) -> Exercise {

    override fun invoke(entity: ExerciseEntity): Exercise {
        return Exercise
            .Builder()
            .id(entity.id)
            .name(entity.name)
            .description(entity.description)
            .difficulty(entity.difficulty)
            .exerciseType(entity.exerciseType)
            .met(entity.met)
            .categories(entity.categories)
            .muscleGroups(entity.muscleGroups)
            .equipmentRequired(entity.equipmentRequired)
            .instructions(entity.instructions)
            .category(entity.category)
            .build()
    }
}
