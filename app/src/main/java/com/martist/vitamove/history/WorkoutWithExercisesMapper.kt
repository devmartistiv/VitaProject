package com.martist.vitamove.history

import com.martist.vitamove.VitaMoveApplication
import com.martist.vitamove.core.data.local.AppDatabase
import com.martist.vitamove.exercise.data.local.mappers.ExerciseDBToExercise
import com.martist.vitamove.exercise.ui.model.Exercise
import com.martist.vitamove.workout.data.dto.WorkoutWithExercises
import com.martist.vitamove.workout.data.model.UserWorkout


class WorkoutWithExercisesMapper(

) {
    val exerciseDao = AppDatabase.getInstance(VitaMoveApplication.context).exerciseDao()
    private val exerciseMapper = ExerciseDBToExercise()
    fun map(workouts: List<WorkoutWithExercises>): List<UserWorkout> {
        if (workouts.isEmpty())
            return emptyList()
        val exercisesById = loadBaseExercises(workouts)
        return workouts.map { dto ->
            val workoutExercises = dto.exercises.map { exerciseWithSets ->
                val entity = exerciseWithSets.exerciseEntity
                val baseExercise = exercisesById[entity.baseExerciseId]
                    ?: placeholderExercise(entity.baseExerciseId)
                val sets = exerciseWithSets.sets.map { it.toModel() }
                entity.toModel(baseExercise, sets)
            }
            dto.workout.toModel(workoutExercises)
        }
    }

    fun map(workout: WorkoutWithExercises): UserWorkout =
        map(listOf(workout)).first()

    private fun loadBaseExercises(
        workouts: List<WorkoutWithExercises>
    ): Map<String, Exercise> {
        val ids = workouts
            .flatMap { it.exercises }
            .map { it.exerciseEntity.baseExerciseId }
            .distinct()
        if (ids.isEmpty()) return emptyMap()
        return exerciseDao
            .getExercisesByIds(ids)
            .associate { entity ->
                entity.id to exerciseMapper(entity)
            }
    }

    private fun placeholderExercise(baseExerciseId: String): Exercise =
        Exercise.Builder()
            .id(baseExerciseId)
            .name("Упражнение не найдено")
            .build()
}