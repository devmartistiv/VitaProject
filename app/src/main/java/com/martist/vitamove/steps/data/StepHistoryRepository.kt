package com.martist.vitamove.steps.data

import android.content.Context
import android.util.Log
import com.martist.vitamove.core.data.local.AppDatabase
import com.martist.vitamove.steps.data.local.dao.StepHistoryDao
import com.martist.vitamove.steps.data.local.entities.StepHistoryEntity
import org.threeten.bp.DayOfWeek
import org.threeten.bp.LocalDate
import org.threeten.bp.temporal.TemporalAdjusters
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import java.util.concurrent.Callable
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors
import java.util.concurrent.Future
import kotlin.math.min


class StepHistoryRepository private constructor(context: Context?) {
    private val stepHistoryDao: StepHistoryDao
    private val executor: ExecutorService


    init {
        val db = AppDatabase.getInstance(context)
        stepHistoryDao = db.stepHistoryDao()
        executor = Executors.newSingleThreadExecutor()
    }


    fun saveStepsForToday(stepCount: Int) {
        saveStepsForDate(formatDate(LocalDate.now()), stepCount)
    }

    val stepsForLastWeek: MutableList<Int?>
        get() {

            val weeklySteps: MutableList<Int?> =
                ArrayList<Int?>(mutableListOf<Int?>(0, 0, 0, 0, 0, 0, 0))

            try {

                val MAX_REASONABLE_STEPS = 100000

                val startDate = formatDate(
                    LocalDate.now().with(
                        TemporalAdjusters.previousOrSame(
                            DayOfWeek.MONDAY
                        )
                    )
                )
                val endDate = formatDate(
                    LocalDate.now().with(
                        TemporalAdjusters.previousOrSame(
                            DayOfWeek.SUNDAY
                        )
                    )
                )

                Log.d(TAG, "Запрашиваем данные шагов за неделю с " + startDate + " по " + endDate)


//                val historyEntities = stepHistoryDao.getHistoryBetweenDates(startDate, endDate)


                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
//                for (entity in historyEntities) {
//                    val entityDate = Calendar.getInstance()
//                    entityDate.setTime(sdf.parse(entity.date))
//
//                    val entityDayOfWeek = entityDate.get(Calendar.DAY_OF_WEEK)
//                    val mondayBasedIndex =
//                        if (entityDayOfWeek == Calendar.SUNDAY) 6 else entityDayOfWeek - 2
//
//                    if (mondayBasedIndex >= 0 && mondayBasedIndex < 7) {
//
//                        val stepCount = min(entity.stepCount, MAX_REASONABLE_STEPS)
//
//
//                        if (entity.stepCount > MAX_REASONABLE_STEPS) {
//                            Log.e(
//                                TAG,
//                                "Обнаружено аномально большое количество шагов для " + entity.date +
//                                        ": " + entity.stepCount + ". Ограничено до " + MAX_REASONABLE_STEPS
//                            )
//                        }
//
//                        weeklySteps.set(mondayBasedIndex, stepCount)
//                    }
//                }


                val stepCounterManager = StepCounterManager.getInstance(AppDatabase.getContext())
                var currentSteps = stepCounterManager.getStepsToday()


                currentSteps = min(currentSteps, MAX_REASONABLE_STEPS)

                val today = Calendar.getInstance()
                val todayDayOfWeek = today.get(Calendar.DAY_OF_WEEK)
                val todayIndex = if (todayDayOfWeek == Calendar.SUNDAY) 6 else todayDayOfWeek - 2

                if (todayIndex >= 0 && todayIndex < 7 && weeklySteps.get(todayIndex)!! < currentSteps) {
                    weeklySteps.set(todayIndex, currentSteps)
                    Log.d(TAG, "Обновлены шаги за сегодня до актуального значения: " + currentSteps)
                }

                Log.d(TAG, "Получена история шагов за неделю: " + weeklySteps)
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении истории шагов за неделю: " + e.message, e)
            }

            return weeklySteps
        }

    val stepsForCurrentMonth: MutableList<Int?>
        get() {
            try {

                val calendar = Calendar.getInstance()
                val daysInMonth = calendar.getActualMaximum(Calendar.DAY_OF_MONTH)
                val currentDay = calendar.get(Calendar.DAY_OF_MONTH)


                val monthlySteps: MutableList<Int?> = ArrayList<Int?>()
                for (i in 0..<daysInMonth) {
                    monthlySteps.add(0)
                }

                val startDate = formatDate(LocalDate.now().withDayOfMonth(1))
                val endDate = formatDate(LocalDate.now().with(TemporalAdjusters.lastDayOfMonth()))

                Log.d(TAG, "Запрашиваем данные шагов за месяц с " + startDate + " по " + endDate)


                val historyEntities = stepHistoryDao.getHistoryBetweenDates(startDate, endDate)


                val MAX_REASONABLE_STEPS = 100000


                val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
                for (entity in historyEntities) {

                    val entityDate = Calendar.getInstance()
                    entityDate.setTime(sdf.parse(entity.date))


                    val dayOfMonth = entityDate.get(Calendar.DAY_OF_MONTH)


                    val stepCount = min(entity.stepCount, MAX_REASONABLE_STEPS)



                    monthlySteps.set(dayOfMonth - 1, stepCount)
                }


                if (currentDay > 0 && currentDay <= daysInMonth) {

                    val stepsToday: Int = monthlySteps.get(currentDay - 1)!!
                    if (stepsToday == 0) {

                        val stepCounterManager =
                            StepCounterManager.getInstance(AppDatabase.getContext())
                        var currentSteps = stepCounterManager.getStepsToday()


                        currentSteps = min(currentSteps, MAX_REASONABLE_STEPS)


                        monthlySteps.set(currentDay - 1, currentSteps)
                        Log.d(TAG, "Добавлены текущие данные шагов за сегодня: " + currentSteps)
                    }
                }

                Log.d(TAG, "Получена история шагов за месяц: " + monthlySteps)
                return monthlySteps
            } catch (e: Exception) {
                Log.e(TAG, "Ошибка при получении истории шагов за месяц: " + e.message, e)
                return ArrayList<Int?>()
            }
        }


    fun saveStepsForDate(date: String, stepCount: Int) {

        val MAX_REASONABLE_STEPS = 100000


        val validatedStepCount = min(stepCount, MAX_REASONABLE_STEPS)

        executor.execute(Runnable {
            try {
                var entity = stepHistoryDao.getStepHistoryForDate(date)
                if (entity == null) {

                    entity = StepHistoryEntity(date, validatedStepCount)
                    stepHistoryDao.insert(entity)
                } else {
                    if (entity.stepCount < validatedStepCount) {

                        entity.stepCount = validatedStepCount
                        stepHistoryDao.update(entity)
                    }
                }
            } catch (e: Exception) {
                Log.e(
                    TAG,
                    "Ошибка при сохранении истории шагов для даты " + date + ": " + e.message,
                    e
                )
            }
        })
    }


    private fun formatDate(date: LocalDate): String {
        return date.toString()
    }


    companion object {
        private const val TAG = "StepHistoryRepository"

        private var instance: StepHistoryRepository? = null


        @Synchronized
        fun getInstance(context: Context?): StepHistoryRepository {
            if (instance == null) {
                instance = StepHistoryRepository(context)
            }
            return instance!!
        }
    }
}