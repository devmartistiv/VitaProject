package com.martist.vitamove.programs.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.martist.vitamove.programs.data.local.entities.ProgramDayEntity;

import java.util.List;

@Dao
public interface ProgramDayDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProgramDayEntity day);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProgramDayEntity> days);


    @Update
    void update(ProgramDayEntity day);


    @Query("SELECT * FROM program_days WHERE id = :dayId")
    ProgramDayEntity getById(String dayId);


    @Query("SELECT * FROM program_days WHERE program_id = :programId ORDER BY day_number")
    List<ProgramDayEntity> getAllByProgramId(String programId);


    @Query("SELECT * FROM program_days WHERE program_id = :programId AND day_number = :dayNumber LIMIT 1")
    ProgramDayEntity getByProgramIdAndDayNumber(String programId, int dayNumber);


    @Query("DELETE FROM program_days WHERE id = :dayId")
    void deleteById(String dayId);


    @Query("DELETE FROM program_days WHERE program_id = :programId")
    void deleteAllByProgramId(String programId);


    @Query("DELETE FROM program_days")
    void deleteAll();


    @Query("SELECT COUNT(*) FROM program_days WHERE program_id = :programId")
    int countByProgramId(String programId);


    @Transaction
    default void replaceDaysForProgram(String programId, List<ProgramDayEntity> days) {
        deleteAllByProgramId(programId);
        insertAll(days);
    }
} 