package com.martist.vitamove.weight.ui;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.martist.vitamove.weight.data.UserWeightRepository;
import com.martist.vitamove.weight.data.local.entities.UserWeightEntity;

import java.util.Date;
import java.util.List;


public class UserWeightViewModel extends AndroidViewModel {

    private final UserWeightRepository repository;
    private final MutableLiveData<Boolean> isSyncing = new MutableLiveData<>(false);
    private final MutableLiveData<String> syncStatus = new MutableLiveData<>("");


    public UserWeightViewModel(@NonNull Application application) {
        super(application);
        repository = new UserWeightRepository(application);
    }


    public void addWeightRecord(float weight, Date date, String notes) {
        repository.addWeightRecord(weight, date, notes);
    }


    public void addWeightRecord(float weight, String notes) {
        repository.addWeightRecord(weight, new Date(), notes);
    }


    public void updateWeightRecord(UserWeightEntity weightEntity) {
        repository.updateWeightRecord(weightEntity);
    }


    public void deleteWeightRecord(UserWeightEntity weightEntity) {
        repository.deleteWeightRecord(weightEntity);
    }


    public LiveData<List<UserWeightEntity>> getAllWeightRecords() {
        return repository.getAllWeightRecords();
    }


    public LiveData<UserWeightEntity> getLatestWeightRecord() {
        return repository.getLatestWeightRecord();
    }


    public LiveData<List<UserWeightEntity>> getWeightRecordsForPeriod(Date startDate, Date endDate) {
        return repository.getWeightRecordsInRange(startDate, endDate);
    }


    public LiveData<UserWeightEntity> getWeightRecordById(String id) {
        return repository.getWeightRecordById(id);
    }


    public void syncAllUnsyncedRecords() {
        repository.syncAllUnsyncedRecords();
    }


    public void syncFromSupabase() {
        isSyncing.setValue(true);
        syncStatus.setValue("Загрузка данных...");

        repository.syncFromSupabase(new UserWeightRepository.SyncCallback() {
            @Override
            public void onSyncCompleted(boolean success, String message) {
                isSyncing.postValue(false);
                syncStatus.postValue(message);
            }
        });
    }


    public LiveData<Boolean> getIsSyncing() {
        return isSyncing;
    }


    public LiveData<String> getSyncStatus() {
        return syncStatus;
    }


    public void addWeightRecordOnlyToSupabase(float weight, Date date, String notes) {
        repository.addWeightRecordOnlyToSupabase(weight, date, notes);
    }


    public void addWeightRecordOnlyToSupabase(float weight, String notes) {
        repository.addWeightRecordOnlyToSupabase(weight, new Date(), notes);
    }
} 