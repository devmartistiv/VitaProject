package com.martist.vitamove.workout.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workout_plans_cache")
public class WorkoutPlanEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    public String id;

    @ColumnInfo(name = "user_id")
    public String userId;

    @ColumnInfo(name = "name")
    public String name;

    @ColumnInfo(name = "planned_date")
    public String plannedDate;

    @ColumnInfo(name = "program_id", index = true)
    public String programId;

    @ColumnInfo(name = "program_day_id")
    public String programDayId;

    @ColumnInfo(name = "status")
    public String status;

    @ColumnInfo(name = "notes")
    public String notes;

    @ColumnInfo(name = "created_at")
    public String createdAt;

    @ColumnInfo(name = "updated_at")
    public String updatedAt;


    @ColumnInfo(name = "cached_at")
    public long cachedAt;


    public WorkoutPlanEntity() {
    }


} 