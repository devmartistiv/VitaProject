package com.martist.vitamove.workout.data.dao

import android.util.Log
import androidx.lifecycle.LiveData
import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import androidx.room.Update
import com.martist.vitamove.exercise.ui.model.Exercise
import com.martist.vitamove.exercise.ui.model.ExerciseSet
import com.martist.vitamove.history.ExerciseNoteHistoryRaw
import com.martist.vitamove.set.ExerciseSetEntity
import com.martist.vitamove.workout.data.dto.WorkoutWithExercises
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity
import com.martist.vitamove.workout.data.model.UserWorkout
import com.martist.vitamove.workout.data.model.WorkoutExercise
import kotlinx.coroutines.flow.Flow
import java.util.UUID

@Dao
abstract class WorkoutDao {

    @Transaction
    @Query("SELECT * FROM user_workouts  WHERE start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    abstract fun getWorkoutsWithExercisesAndSetsInTimeRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<WorkoutWithExercises>>

    @Query("SELECT MAX(user_workouts.start_time) FROM user_workouts INNER JOIN workout_exercises ON workout_id = user_workouts.id  WHERE user_workouts.start_time>= :fromTime GROUP BY date(user_workouts.start_time/1000,'unixepoch','localtime')")
    abstract fun getWorkoutDays(fromTime: Long): Flow<List<Long>>

    @Query("SELECT * FROM user_workouts  WHERE start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    abstract fun getWorkoutsInTimeRange(
        startTime: Long,
        endTime: Long
    ): Flow<List<UserWorkoutEntity>>


    @Query("SELECT COUNT(*) FROM user_workouts")
    abstract suspend fun count(): Int

    @Transaction
    @Query("SELECT * FROM user_workouts")
    abstract fun getWorkoutsWithExercisesAndSets(): List<WorkoutWithExercises>

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    abstract fun insertWorkout(workout: UserWorkoutEntity)

    @Update
    abstract fun updateWorkout(workout: UserWorkoutEntity)

    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND end_time IS NULL ORDER BY start_time DESC LIMIT 1")
    abstract fun getActiveWorkoutEntity(userId: String): UserWorkoutEntity?


    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND end_time IS NULL ORDER BY start_time DESC")
    abstract fun getAllUnfinishedWorkouts(userId: String): List<UserWorkoutEntity>

    @Query("DELETE FROM user_workouts WHERE id = :workoutId")
    abstract fun deleteWorkoutById(workoutId: String)

    @Query("DELETE FROM user_workouts")
    abstract fun deleteAllWorkouts()

    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC LIMIT :limit OFFSET :offset")
    abstract fun getWorkoutsByTimeRange(
        userId: String?,
        startTime: Long,
        endTime: Long,
        offset: Int,
        limit: Int
    ): List<UserWorkoutEntity>


    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    abstract fun getAllWorkoutsByTimeRange(
        userId: String?,
        startTime: Long,
        endTime: Long
    ): List<UserWorkoutEntity>


    @Query("SELECT * FROM user_workouts WHERE user_id = :userId AND start_time >= :startTime AND start_time <= :endTime ORDER BY start_time DESC")
    abstract fun getAllWorkoutsByTimeRangeLiveData(
        userId: String?,
        startTime: Long,
        endTime: Long
    ): LiveData<List<UserWorkoutEntity>>


    @Query("SELECT * FROM user_workouts WHERE id = :workoutId LIMIT 1")
    abstract fun getWorkoutEntityById(workoutId: String): UserWorkoutEntity


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertWorkoutExercises(exercises: List<WorkoutExerciseEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    abstract fun insertWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Update
    abstract fun updateWorkoutExercise(exercise: WorkoutExerciseEntity)

    @Query("SELECT * FROM workout_exercises WHERE workout_id = :workoutId ORDER BY order_number ASC")
    abstract fun getExercisesForWorkout(workoutId: String): List<WorkoutExerciseEntity>


    @Query("SELECT * FROM workout_exercises WHERE workout_id = :workoutId ORDER BY order_number ASC")
    abstract fun getExercisesForWorkoutLiveData(workoutId: String): LiveData<List<WorkoutExerciseEntity>>

    @Query("DELETE FROM workout_exercises WHERE id = :exerciseId")
    abstract fun deleteWorkoutExerciseById(exerciseId: String?)


    @Query("DELETE FROM workout_exercises WHERE workout_id = :workoutId")
    abstract fun deleteAllExercisesForWorkout(workoutId: String)


    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    abstract fun insertExerciseSets(sets: List<ExerciseSetEntity>)

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    abstract fun insertExerciseSet(set: ExerciseSetEntity)

    @Update
    abstract fun updateExerciseSet(set: ExerciseSetEntity)

    @Query("SELECT * FROM exercise_sets WHERE workout_exercise_id = :exerciseId ORDER BY set_number ASC")
    abstract fun getSetsForExercise(exerciseId: String): List<ExerciseSetEntity>


    @Query("SELECT * FROM exercise_sets WHERE workout_exercise_id = :exerciseId ORDER BY set_number ASC")
    abstract fun getSetsForExerciseLiveData(exerciseId: String): LiveData<List<ExerciseSetEntity>>

    @Query("DELETE FROM exercise_sets WHERE workout_exercise_id = :exerciseId")
    abstract fun deleteSetsForExercise(exerciseId: String)


    @Query("DELETE FROM exercise_sets WHERE workout_exercise_id IN (SELECT id FROM workout_exercises WHERE workout_id = :workoutId)")
    abstract fun deleteAllSetsForWorkout(workoutId: String)


    fun getFullActiveWorkout(userId: String, repoHelper: WorkoutRepositoryHelper): UserWorkout? {
        val workoutEntity = getActiveWorkoutEntity(userId) ?: return null

        val workoutExercises = mutableListOf<WorkoutExercise>()
        val exerciseEntities = getExercisesForWorkout(workoutEntity.getId())

        if (exerciseEntities.isEmpty()) {
            return workoutEntity.toModel(workoutExercises)
        }


        val exerciseIds: MutableList<String?> = ArrayList<String?>()
        for (entity in exerciseEntities) {
            exerciseIds.add(entity.getBaseExerciseId())
        }


        val exerciseList =
            repoHelper.getExercisesByIds(exerciseIds)



        for (exerciseEntity in exerciseEntities) {
            var baseExercise =
                exerciseList.stream()
                    .filter { v: Exercise? -> v!!.getId() == exerciseEntity.getBaseExerciseId() }
                    .findAny().orElse(null)

            if (baseExercise == null) {
                baseExercise = Exercise.Builder()
                    .id(exerciseEntity.getBaseExerciseId())
                    .name("Упражнение не найдено")
                    .build()
            }

            val setEntities = getSetsForExercise(exerciseEntity.getId())
            val sets: MutableList<ExerciseSet?> = ArrayList<ExerciseSet?>()
            for (setEntity in setEntities) {
                sets.add(setEntity.toModel())
            }

            workoutExercises.add(exerciseEntity.toModel(baseExercise, sets))
        }

        return workoutEntity.toModel(workoutExercises)
    }


    @Transaction
    open fun saveFullWorkout(workout: UserWorkout?) {
        if (workout == null) return

        val workoutEntity = UserWorkoutEntity.fromModel(workout)
        insertWorkout(workoutEntity)

        if (workout.getExercises() != null) {
            val exerciseEntities: MutableList<WorkoutExerciseEntity> =
                ArrayList<WorkoutExerciseEntity>()
            val allSetEntities: MutableList<ExerciseSetEntity> = ArrayList<ExerciseSetEntity>()

            for (exerciseModel in workout.getExercises()) {
                val exerciseEntity = WorkoutExerciseEntity.fromModel(exerciseModel, workout.getId())
                exerciseEntities.add(exerciseEntity)

                if (exerciseModel.getSetsCompleted() != null) {
                    for (setModel in exerciseModel.getSetsCompleted()) {

                        if (setModel.getExerciseId() == null && exerciseModel.getExercise() != null) {
                            setModel.setExerciseId(exerciseModel.getExercise().getId())
                        }

                        val setEntity =
                            ExerciseSetEntity.fromModel(setModel, exerciseEntity.getId())
                        allSetEntities.add(setEntity)


                    }
                }
            }
            insertWorkoutExercises(exerciseEntities)

            insertExerciseSets(allSetEntities)
        }
    }


    @Transaction
    open fun updateSingleSet(set: ExerciseSet?) {
        if (set == null || set.getWorkoutExerciseId() == null) return


        if (set.getExerciseId() == null) {
            try {
                val exercise = getWorkoutExerciseById(set.getWorkoutExerciseId())
                if (exercise != null) {
                    set.setExerciseId(exercise.getBaseExerciseId())
                }
            } catch (e: Exception) {
                Log.e(TAG, "updateSingleSet: Ошибка при получении exercise_id", e)
            }
        }

        val setEntity = ExerciseSetEntity.fromModel(set, set.getWorkoutExerciseId())
        updateExerciseSet(setEntity)
    }


    @Query("SELECT * FROM workout_exercises WHERE id = :exerciseId LIMIT 1")
    abstract fun getWorkoutExerciseById(exerciseId: String?): WorkoutExerciseEntity?

    @Update
    abstract fun updateExerciseSetList(setEntities: List<ExerciseSetEntity>)


    @Transaction
    open fun addSetToExercise(set: ExerciseSet?) {
        if (set == null || set.getWorkoutExerciseId() == null) return


        if (set.getId() == null) {
            set.setId(UUID.randomUUID().toString())
        }


        if (set.getExerciseId() == null) {
            try {
                val exercise = getWorkoutExerciseById(set.getWorkoutExerciseId())
                if (exercise != null) {
                    set.setExerciseId(exercise.getBaseExerciseId())

                }
            } catch (e: Exception) {
                Log.e(TAG, "addSetToExercise: Ошибка при получении exercise_id", e)
            }
        }

        val setEntity = ExerciseSetEntity.fromModel(set, set.getWorkoutExerciseId())
        insertExerciseSet(setEntity)
    }


    @Transaction
    open fun addExerciseToWorkout(exercise: WorkoutExercise?, workoutId: String) {
        if (exercise == null || exercise.getExercise() == null) return
        val exerciseEntity = WorkoutExerciseEntity.fromModel(exercise, workoutId)
        insertWorkoutExercise(exerciseEntity)

        val exerciseId = exercise.getExercise().getId()


        if (exercise.getSetsCompleted() != null && !exercise.getSetsCompleted().isEmpty()) {
            val setEntities: MutableList<ExerciseSetEntity> = ArrayList<ExerciseSetEntity>()
            for (setModel in exercise.getSetsCompleted()) {

                setModel.setExerciseId(exerciseId)


                if (setModel.getCreatedAt() == null) {
                    setModel.setCreatedAt(System.currentTimeMillis())
                }

                val entity = ExerciseSetEntity.fromModel(setModel, exerciseEntity.getId())
                setEntities.add(entity)

            }


            insertExerciseSets(setEntities)
        }
    }


    @Transaction
    open fun deleteFullWorkoutExercise(exerciseId: String) {
        deleteSetsForExercise(exerciseId)
        deleteWorkoutExerciseById(exerciseId)
    }


    @Transaction
    open fun deleteFullWorkout(workoutId: String) {
        val exercises = getExercisesForWorkout(workoutId)
        for (exercise in exercises) {
            deleteSetsForExercise(exercise.getId())
        }

        deleteWorkoutById(workoutId)
    }


    interface WorkoutRepositoryHelper {
        fun getExerciseDetailsSync(exerciseId: String?): Exercise?


        fun getExercisesByIds(exerciseIds: MutableList<String?>?): List<Exercise?>
    }


    @Query("SELECT weight FROM exercise_sets WHERE exercise_id = :exerciseId AND weight IS NOT NULL AND weight > 0 ORDER BY created_at DESC LIMIT 1")
    abstract fun getLastWeightForExercise(exerciseId: String?): Float?


    @Query("SELECT reps FROM exercise_sets WHERE exercise_id = :exerciseId AND reps IS NOT NULL AND reps > 0 ORDER BY created_at DESC LIMIT 1")
    abstract fun getLastRepsForExercise(exerciseId: String?): Int?


    @Query("SELECT COALESCE(SUM(total_calories), 0) FROM user_workouts WHERE user_id = :userId AND start_time >= :startTime AND start_time <= :endTime AND end_time IS NOT NULL")
    abstract fun getTotalCaloriesForTimeRange(userId: String?, startTime: Long, endTime: Long): Int


    @Query("SELECT COALESCE(SUM(total_calories), 0) FROM user_workouts WHERE user_id = :userId AND start_time >= :todayStart AND start_time <= :todayEnd AND end_time IS NOT NULL")
    abstract fun getTotalCaloriesForToday(userId: String?, todayStart: Long, todayEnd: Long): Int


    @Query(
        ("SELECT we.notes, " +
                "CASE " +
                "  WHEN uw.start_time > 0 THEN uw.start_time " +
                "  WHEN uw.end_time > 0 THEN uw.end_time " +
                "  ELSE strftime('%s', 'now') * 1000 " +
                "END as start_time, " +
                "uw.name as workout_name, we.id as exercise_id, uw.id as workout_id " +
                "FROM workout_exercises we " +
                "INNER JOIN user_workouts uw ON we.workout_id = uw.id " +
                "WHERE we.base_exercise_id = :baseExerciseId " +
                "AND uw.user_id = :userId " +
                "AND we.notes IS NOT NULL " +
                "AND TRIM(we.notes) != '' " +
                "ORDER BY start_time DESC")
    )
    abstract fun getExerciseNotesHistory(
        baseExerciseId: String?,
        userId: String?
    ): MutableList<ExerciseNoteHistoryRaw?>?


    @Query(
        ("SELECT we.notes, strftime('%s', 'now') * 1000 as start_time, 'Тренировка' as workout_name, we.id as exercise_id, we.workout_id as workout_id " +
                "FROM workout_exercises we " +
                "WHERE we.base_exercise_id = :baseExerciseId " +
                "AND we.notes IS NOT NULL " +
                "AND TRIM(we.notes) != '' " +
                "ORDER BY we.id DESC")
    )
    abstract fun getExerciseNotesHistorySimple(baseExerciseId: String?): MutableList<ExerciseNoteHistoryRaw?>?


    @Query(
        ("SELECT we.notes, " +
                "CASE " +
                "  WHEN uw.start_time > 0 THEN uw.start_time " +
                "  WHEN uw.end_time > 0 THEN uw.end_time " +
                "  ELSE strftime('%s', 'now') * 1000 " +
                "END as start_time, " +
                "COALESCE(uw.name, 'Без названия') as workout_name, we.id as exercise_id, uw.id as workout_id " +
                "FROM workout_exercises we " +
                "LEFT JOIN user_workouts uw ON we.workout_id = uw.id " +
                "WHERE we.base_exercise_id = :baseExerciseId " +
                "ORDER BY start_time DESC")
    )
    abstract fun getAllExerciseNotesForDebug(baseExerciseId: String?): MutableList<ExerciseNoteHistoryRaw?>?


    @Query("SELECT COUNT(*) FROM exercise_sets WHERE workout_exercise_id = :workoutExerciseId AND completed = 1")
    abstract fun getCompletedSetsCount(workoutExerciseId: String?): Int


    @Query("SELECT COALESCE(SUM(duration_seconds), 0) / 60 FROM exercise_sets WHERE workout_exercise_id = :workoutExerciseId")
    abstract fun getExerciseDurationMinutes(workoutExerciseId: String?): Long


    @get:Query("SELECT * FROM user_workouts WHERE is_synced = 0 ORDER BY local_created_at ASC")
    abstract val unsyncedWorkouts: List<UserWorkoutEntity>


    @Query("UPDATE user_workouts SET is_synced = 1 WHERE id = :workoutId")
    abstract fun markWorkoutAsSynced(workoutId: String)


    @Query("UPDATE user_workouts SET is_synced = 0 WHERE id = :workoutId")
    abstract fun markWorkoutAsUnsynced(workoutId: String)


    @Transaction
    open fun deleteWorkoutCompletely(workoutId: String) {


        deleteAllSetsForWorkout(workoutId)



        deleteAllExercisesForWorkout(workoutId)



        deleteWorkoutById(workoutId)

    }

    companion object {
        private const val TAG = "WorkoutDao"
    }
}