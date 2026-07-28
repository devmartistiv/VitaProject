package com.martist.vitamove.programs.data.local.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Transaction;
import androidx.room.Update;

import com.martist.vitamove.programs.data.local.entities.ProgramEntity;

import java.util.List;

@Dao
public interface ProgramDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(ProgramEntity program);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<ProgramEntity> programs);


    @Update
    void update(ProgramEntity program);


    @Query("SELECT * FROM programs WHERE id = :programId")
    ProgramEntity getById(String programId);


    @Query("SELECT * FROM programs WHERE user_id = :userId ORDER BY created_at DESC")
    List<ProgramEntity> getAllByUserId(String userId);


    @Query("SELECT * FROM programs WHERE user_id = :userId AND is_active = 1 ORDER BY created_at DESC")
    List<ProgramEntity> getActiveByUserId(String userId);


    @Query("DELETE FROM programs WHERE id = :programId")
    void deleteById(String programId);


    @Query("DELETE FROM programs WHERE user_id = :userId")
    void deleteAllByUserId(String userId);


    @Query("DELETE FROM programs")
    void deleteAll();


    @Query("SELECT COUNT(*) FROM programs")
    int count();


    @Query("SELECT EXISTS(SELECT 1 FROM programs WHERE id = :programId LIMIT 1)")
    boolean exists(String programId);


    @Transaction
    default void replaceAll(List<ProgramEntity> programs) {
        deleteAll();
        insertAll(programs);
    }
} 