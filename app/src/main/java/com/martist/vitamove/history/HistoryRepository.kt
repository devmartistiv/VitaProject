package com.martist.vitamove.history

import com.martist.vitamove.VitaMoveApplication
import com.martist.vitamove.core.data.local.AppDatabase
import com.martist.vitamove.core.data.remote.SupabaseClient
import com.martist.vitamove.core.domain.utils.Constants
import com.martist.vitamove.workout.data.dto.WorkoutWithExercises
import com.martist.vitamove.workout.data.entities.UserWorkoutEntity
import com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository
import dagger.hilt.android.internal.Contexts.getApplication
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class HistoryRepository @Inject constructor() {
    val workoutDao = AppDatabase.getInstance(VitaMoveApplication.context).workoutDao()
    val exerciseDao = AppDatabase.getInstance(VitaMoveApplication.context).exerciseDao()
    val GetMonthStartEndTimeUseCase = GetMonthStartEndTimeUseCase()
    val supabaseClient: SupabaseClient = SupabaseClient.getInstance(
        Constants.SUPABASE_CLIENT_ID,
        Constants.SUPABASE_CLIENT_SECRET
    )

    fun loadWorkoutsWithExercisesForSelectedMonth(
        year: Int,
        month: Int
    ): Flow<List<WorkoutWithExercises>> {

        val (start, end) = GetMonthStartEndTimeUseCase(year, month)
        return workoutDao.getWorkoutsWithExercisesAndSetsInTimeRange(start, end)
    }

    fun loadWorkoutsForSelectedMonth(year: Int, month: Int): Flow<List<UserWorkoutEntity>> {

        val (start, end) = GetMonthStartEndTimeUseCase(year, month)
        return workoutDao.getWorkoutsInTimeRange(start, end)
    }

    fun getWorkoutDays(fromTime: Long) = workoutDao.getWorkoutDays(fromTime)

    suspend fun isLocalWorkoutsEmpty() = workoutDao.count() == 0

    private fun syncWorkoutsFromSupabase() {
        val userId =
            (getApplication(VitaMoveApplication.context) as VitaMoveApplication).getCurrentUserId()
                ?: return


        val supabaseRepo = SupabaseWorkoutRepository(supabaseClient)


        supabaseRepo.syncUserWorkouts(
            userId,
            {

            },
            {

            }
        )

    }

    fun getWorkoutsByCalendarDay(dayStart: Long, dayEnd: Long): Flow<List<WorkoutWithExercises>> {
        return workoutDao.getWorkoutsWithExercisesAndSetsInTimeRange(dayStart, dayEnd)

    }


}