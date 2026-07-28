package com.martist.vitamove.steps.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.martist.vitamove.steps.data.local.entities.StepHistoryEntity


@Dao
public interface StepHistoryDao {

    @Insert(onConflict = OnConflictStrategy.Companion.REPLACE)
    fun insert(stepHistoryEntity: StepHistoryEntity)


    @Update
    fun update(stepHistoryEntity: StepHistoryEntity)


    @Query("SELECT * FROM step_history WHERE date = :date LIMIT 1")
    fun getStepHistoryForDate(date: String): StepHistoryEntity


    @Query("SELECT * FROM step_history WHERE date BETWEEN :startDate AND :endDate ORDER BY date ASC")
    fun getHistoryBetweenDates(
        startDate: String,
        endDate: String
    ): List<StepHistoryEntity>
}