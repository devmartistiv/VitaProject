package com.martist.vitamove.programs.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;


@Entity(tableName = "program_exercises",
        foreignKeys = @ForeignKey(
                entity = ProgramDayEntity.class,
                parentColumns = "id",
                childColumns = "day_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index("day_id"), @Index("exercise_id")})
public class ProgramExerciseEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @NonNull
    @ColumnInfo(name = "day_id")
    private String dayId;

    @NonNull
    @ColumnInfo(name = "exercise_id")
    private String exerciseId;

    @ColumnInfo(name = "order_number")
    private int orderNumber;

    @ColumnInfo(name = "target_sets")
    private int targetSets;

    @ColumnInfo(name = "target_reps")
    private String targetReps;

    @ColumnInfo(name = "target_weight")
    private String targetWeight;

    @ColumnInfo(name = "rest_seconds")
    private int restSeconds;

    @ColumnInfo(name = "template_exercise_id")
    private String templateExerciseId;

    @ColumnInfo(name = "notes")
    private String notes;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;


    public ProgramExerciseEntity() {
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    @NonNull
    public String getDayId() {
        return dayId;
    }

    public void setDayId(@NonNull String dayId) {
        this.dayId = dayId;
    }

    @NonNull
    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(@NonNull String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public int getTargetSets() {
        return targetSets;
    }

    public void setTargetSets(int targetSets) {
        this.targetSets = targetSets;
    }

    public String getTargetReps() {
        return targetReps;
    }

    public void setTargetReps(String targetReps) {
        this.targetReps = targetReps;
    }

    public String getTargetWeight() {
        return targetWeight;
    }

    public void setTargetWeight(String targetWeight) {
        this.targetWeight = targetWeight;
    }

    public int getRestSeconds() {
        return restSeconds;
    }

    public void setRestSeconds(int restSeconds) {
        this.restSeconds = restSeconds;
    }

    public String getTemplateExerciseId() {
        return templateExerciseId;
    }

    public void setTemplateExerciseId(String templateExerciseId) {
        this.templateExerciseId = templateExerciseId;
    }

    public String getNotes() {
        return notes;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public String getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(String createdAt) {
        this.createdAt = createdAt;
    }

    public String getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(String updatedAt) {
        this.updatedAt = updatedAt;
    }
} 