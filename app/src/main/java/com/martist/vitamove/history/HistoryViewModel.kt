package com.martist.vitamove.history

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.asLiveData
import androidx.lifecycle.switchMap
import com.martist.vitamove.workout.data.dto.WorkoutWithExercises
import com.martist.vitamove.workout.data.model.WorkoutExercise
import com.prolificinteractive.materialcalendarview.CalendarDay
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject

@HiltViewModel
class HistoryViewModel @Inject constructor(val repository: HistoryRepository) : ViewModel() {


    fun loadWorkoutsForSelectedMonth(year: Int, month: Int) =
        repository.loadWorkoutsForSelectedMonth(year, month).asLiveData()

    fun loadFullWorkoutsForSelectedMonth(year: Int, month: Int) =
        repository.loadWorkoutsWithExercisesForSelectedMonth(year, month).asLiveData()

    val sixMonth = LocalDate.now().minusMonths(6).atStartOfDay(ZoneId.systemDefault()).toInstant()
        .toEpochMilli()

    val workoutDays = repository.getWorkoutDays(sixMonth).asLiveData()
    private val selectedMonth = MutableLiveData(
        MonthSelection(year = LocalDate.now().year, month = LocalDate.now().monthValue)
    )
    private val selectedDay = MutableLiveData<Pair<Long, Long>>()

    var workoutsByDay = selectedDay.switchMap { day ->
        repository.getWorkoutsByCalendarDay(day.first, day.second).asLiveData()
    }

    val monthWorkouts: LiveData<List<WorkoutWithExercises>> = selectedMonth.switchMap { sel ->
        repository
            .loadWorkoutsWithExercisesForSelectedMonth(sel.year, sel.month)
            .asLiveData()
    }


    fun onMonthChanged(year: Int, month: Int) {
        selectedMonth.postValue(MonthSelection(year, month))
    }

    fun getWorkoutsByCalendarDay(day: CalendarDay) {
        val (dayStart, dayEnd) = GetDayStartEndTimeUseCase()(day)
        selectedDay.postValue(dayStart to dayEnd)


    }


    fun saveExercisesForRepeat(exercises: MutableList<WorkoutExercise>): StringBuilder {

        val exerciseIds = StringBuilder()
        for (i in exercises.indices) {
            val workoutExercise = exercises.get(i)
            if (workoutExercise.exercise != null && workoutExercise.exercise
                    .getId() != null
            ) {
                if (i > 0) {
                    exerciseIds.append(",")
                }
                exerciseIds.append(workoutExercise.exercise.id)
            }
        }
        return exerciseIds
    }

}

data class MonthSelection(val year: Int, val month: Int)
