package com.martist.vitamove.workout.data.cache;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.martist.vitamove.workout.data.entities.WorkoutPlanEntity;

import java.util.List;

@Dao
public interface WorkoutPlanDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<WorkoutPlanEntity> plans);


    @Query("SELECT * FROM workout_plans_cache WHERE program_id = :programId ORDER BY planned_date ASC")
    List<WorkoutPlanEntity> getPlansByProgramId(String programId);


    @Query("SELECT * FROM workout_plans_cache WHERE id = :planId")
    List<WorkoutPlanEntity> getPlanById(String planId);


    @Update
    void update(WorkoutPlanEntity entity);


    @Query("DELETE FROM workout_plans_cache WHERE program_id = :programId")
    void deletePlansByProgramId(String programId);


    @Query("DELETE FROM workout_plans_cache")
    void deleteAll();

} 