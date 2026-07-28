package com.martist.vitamove.weight.data.local.dao;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;
import androidx.room.Update;

import com.martist.vitamove.weight.data.local.entities.UserWeightEntity;

import java.util.List;


@Dao
public interface UserWeightDao {


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    long insert(UserWeightEntity weightEntity);


    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<UserWeightEntity> weightEntities);


    @Update
    void update(UserWeightEntity weightEntity);


    @Delete
    void delete(UserWeightEntity weightEntity);


    @Query("SELECT * FROM user_weight_history WHERE user_id = :userId ORDER BY date DESC")
    LiveData<List<UserWeightEntity>> getAllWeightRecordsForUser(String userId);


    @Query("SELECT * FROM user_weight_history WHERE user_id = :userId ORDER BY date DESC LIMIT 1")
    LiveData<UserWeightEntity> getLatestWeightRecord(String userId);


    @Query("SELECT * FROM user_weight_history WHERE user_id = :userId ORDER BY date DESC LIMIT 1")
    UserWeightEntity getLatestWeightRecordDirect(String userId);


    @Query("SELECT * FROM user_weight_history WHERE user_id = :userId AND date BETWEEN :startDate AND :endDate ORDER BY date DESC")
    LiveData<List<UserWeightEntity>> getWeightRecordsInRange(String userId, long startDate, long endDate);


    @Query("SELECT * FROM user_weight_history WHERE id = :id")
    LiveData<UserWeightEntity> getWeightRecordById(String id);


    @Query("SELECT * FROM user_weight_history WHERE id = :id")
    UserWeightEntity getWeightRecordByIdDirect(String id);


    @Query("SELECT * FROM user_weight_history WHERE user_id = :userId AND is_synced = 0")
    List<UserWeightEntity> getUnsyncedRecords(String userId);


    @Query("UPDATE user_weight_history SET is_synced = :isSynced WHERE id = :id")
    void updateSyncStatus(String id, boolean isSynced);


    @Query("SELECT * FROM user_weight_history WHERE user_id = :userId ORDER BY date DESC")
    List<UserWeightEntity> getAllWeightRecordsForUserDirect(String userId);
} 