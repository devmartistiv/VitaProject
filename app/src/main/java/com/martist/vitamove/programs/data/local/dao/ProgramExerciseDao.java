package com.martist.vitamove.programs.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.martist.vitamove.programs.data.local.entities.ProgramExerciseEntity;

import java.util.List;

@Dao
public interface ProgramExerciseDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProgramExerciseEntity exercise);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProgramExerciseEntity> exercises);


    @Update
    void update(ProgramExerciseEntity exercise);


    @Query("SELECT * FROM program_exercises WHERE id = :exerciseId")
    ProgramExerciseEntity getById(String exerciseId);


    @Query("SELECT * FROM program_exercises")
    List<ProgramExerciseEntity> getAll();


    @Query("SELECT * FROM program_exercises WHERE day_id = :dayId ORDER BY order_number")
    List<ProgramExerciseEntity> getAllByDayId(String dayId);


    @Query("SELECT * FROM program_exercises WHERE exercise_id = :exerciseId")
    List<ProgramExerciseEntity> getAllByExerciseId(String exerciseId);


    @Query("DELETE FROM program_exercises WHERE id = :exerciseId")
    void deleteById(String exerciseId);


    @Query("DELETE FROM program_exercises WHERE day_id = :dayId")
    void deleteAllByDayId(String dayId);


    @Query("DELETE FROM program_exercises WHERE exercise_id = :exerciseId")
    void deleteAllByExerciseId(String exerciseId);


    @Query("DELETE FROM program_exercises")
    void deleteAll();


    @Query("SELECT COUNT(*) FROM program_exercises WHERE day_id = :dayId")
    int countByDayId(String dayId);


    @Transaction
    default void replaceExercisesForDay(String dayId, List<ProgramExerciseEntity> exercises) {
        deleteAllByDayId(dayId);
        insertAll(exercises);
    }
} 