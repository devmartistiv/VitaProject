package com.martist.vitamove.workout.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

import com.martist.vitamove.workout.data.model.UserWorkout;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.List;

@Entity(tableName = "user_workouts")
public class UserWorkoutEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @NonNull
    @ColumnInfo(name = "user_id")
    private String userId;

    private String name;

    @ColumnInfo(name = "start_time")
    private long startTime;

    @ColumnInfo(name = "end_time")
    private Long endTime;

    @ColumnInfo(name = "total_calories")
    private int totalCalories;

    private String notes;

    @ColumnInfo(name = "program_id")
    private String programId;

    @ColumnInfo(name = "program_day_number")
    private int programDayNumber;

    @ColumnInfo(name = "program_day_id")
    private String programDayId;

    @ColumnInfo(name = "plan_id")
    private String planId;

    @ColumnInfo(name = "is_synced")
    private boolean isSynced = true;

    @ColumnInfo(name = "local_created_at")
    private Long localCreatedAt;


    public UserWorkoutEntity() {
    }


    @Ignore
    public UserWorkoutEntity(@NonNull String id, @NonNull String userId, String name, long startTime, Long endTime,
                             int totalCalories, String notes, String programId, int programDayNumber,
                             String programDayId, String planId, boolean isSynced, Long localCreatedAt) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.startTime = startTime;
        this.endTime = endTime;
        this.totalCalories = totalCalories;
        this.notes = notes;
        this.programId = programId;
        this.programDayNumber = programDayNumber;
        this.programDayId = programDayId;
        this.planId = planId;
        this.isSynced = isSynced;
        this.localCreatedAt = localCreatedAt;
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getUserId() {
        return userId;
    }

    public void setUserId(@NonNull String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public int getProgramDayNumber() {
        return programDayNumber;
    }

    public void setProgramDayNumber(int programDayNumber) {
        this.programDayNumber = programDayNumber;
    }

    public String getProgramDayId() {
        return programDayId;
    }

    public void setProgramDayId(String programDayId) {
        this.programDayId = programDayId;
    }

    public String getPlanId() {
        return planId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }

    public boolean isSynced() {
        return isSynced;
    }

    public void setSynced(boolean synced) {
        isSynced = synced;
    }

    public Long getLocalCreatedAt() {
        return localCreatedAt;
    }

    public void setLocalCreatedAt(Long localCreatedAt) {
        this.localCreatedAt = localCreatedAt;
    }


    public UserWorkout toModel(List<WorkoutExercise> exercises) {
        return new UserWorkout(
                this.id,
                this.userId,
                this.name,
                this.startTime,
                this.endTime,
                this.totalCalories,
                this.notes,
                this.programId,
                this.programDayNumber,
                this.programDayId,
                this.planId,
                exercises != null ? exercises : new ArrayList<>()
        );
    }


    public static UserWorkoutEntity fromModel(UserWorkout model) {
        if (model.getId() == null) {

            model.setId(java.util.UUID.randomUUID().toString());
        }

        int dayNumber = (model.getProgramDayNumber() != null) ? model.getProgramDayNumber() : 0;

        return new UserWorkoutEntity(
                model.getId(),
                model.getUserId(),
                model.getName(),
                model.getStartTime(),
                model.getEndTime(),
                model.getTotalCalories(),
                model.getNotes(),
                model.getProgramId(),
                dayNumber,
                model.getProgramDayId(),
                model.getPlanId(),
                true,
                null
        );
    }
} 