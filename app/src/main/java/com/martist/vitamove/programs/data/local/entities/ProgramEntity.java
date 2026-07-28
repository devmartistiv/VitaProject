package com.martist.vitamove.programs.data.local.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;


@Entity(tableName = "programs")
public class ProgramEntity {

    @PrimaryKey
    @NonNull
    @ColumnInfo(name = "id")
    private String id;

    @ColumnInfo(name = "user_id")
    private String userId;

    @ColumnInfo(name = "name")
    private String name;

    @ColumnInfo(name = "description")
    private String description;

    @ColumnInfo(name = "type")
    private String type;

    @ColumnInfo(name = "duration_weeks")
    private int durationWeeks;

    @ColumnInfo(name = "days_per_week")
    private int daysPerWeek;

    @ColumnInfo(name = "is_active")
    private boolean isActive;

    @ColumnInfo(name = "start_date")
    private String startDate;

    @ColumnInfo(name = "created_at")
    private String createdAt;

    @ColumnInfo(name = "updated_at")
    private String updatedAt;

    @ColumnInfo(name = "template_id")
    private String templateId;

    @ColumnInfo(name = "periodization_type")
    private String periodizationType;

    @ColumnInfo(name = "current_phase")
    private String currentPhase;

    @ColumnInfo(name = "current_phase_start_date")
    private String currentPhaseStartDate;

    @ColumnInfo(name = "progression_type")
    private String progressionType;


    public ProgramEntity() {
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    public int getDurationWeeks() {
        return durationWeeks;
    }

    public void setDurationWeeks(int durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    public int getDaysPerWeek() {
        return daysPerWeek;
    }

    public void setDaysPerWeek(int daysPerWeek) {
        this.daysPerWeek = daysPerWeek;
    }

    public boolean isActive() {
        return isActive;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
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

    public String getTemplateId() {
        return templateId;
    }

    public void setTemplateId(String templateId) {
        this.templateId = templateId;
    }

    public String getPeriodizationType() {
        return periodizationType;
    }

    public void setPeriodizationType(String periodizationType) {
        this.periodizationType = periodizationType;
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public void setCurrentPhase(String currentPhase) {
        this.currentPhase = currentPhase;
    }

    public String getCurrentPhaseStartDate() {
        return currentPhaseStartDate;
    }

    public void setCurrentPhaseStartDate(String currentPhaseStartDate) {
        this.currentPhaseStartDate = currentPhaseStartDate;
    }

    public String getProgressionType() {
        return progressionType;
    }

    public void setProgressionType(String progressionType) {
        this.progressionType = progressionType;
    }
} 