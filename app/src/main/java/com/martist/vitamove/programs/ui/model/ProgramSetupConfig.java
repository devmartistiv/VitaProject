package com.martist.vitamove.programs.ui.model;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.List;


public class ProgramSetupConfig {
    private String programId;
    private long startDate;
    private List<Integer> workoutDays = new ArrayList<>();
    private boolean remindersEnabled;
    private long reminderTime;
    private boolean autoProgression;


    public ProgramSetupConfig() {

    }


    public ProgramSetupConfig(String programId, long startDate, List<Integer> workoutDays,
                              boolean remindersEnabled, long reminderTime, boolean autoProgression) {
        this.programId = programId;
        this.startDate = startDate;
        this.workoutDays = workoutDays != null ? new ArrayList<>(workoutDays) : new ArrayList<>();
        this.remindersEnabled = remindersEnabled;
        this.reminderTime = reminderTime;
        this.autoProgression = autoProgression;
    }


    public String getProgramId() {
        return programId;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public long getStartDate() {
        return startDate;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public List<Integer> getWorkoutDays() {
        return workoutDays;
    }

    public void setWorkoutDays(List<Integer> workoutDays) {
        this.workoutDays = workoutDays != null ? new ArrayList<>(workoutDays) : new ArrayList<>();
    }

    public boolean isRemindersEnabled() {
        return remindersEnabled;
    }

    public void setRemindersEnabled(boolean remindersEnabled) {
        this.remindersEnabled = remindersEnabled;
    }

    public long getReminderTime() {
        return reminderTime;
    }

    public void setReminderTime(long reminderTime) {
        this.reminderTime = reminderTime;
    }

    public boolean isAutoProgression() {
        return autoProgression;
    }

    public void setAutoProgression(boolean autoProgression) {
        this.autoProgression = autoProgression;
    }


    public boolean isValid() {
        return programId != null && !programId.isEmpty() &&
                startDate > 0 &&
                !workoutDays.isEmpty();
    }

    @NonNull
    @Override
    public String toString() {
        return "ProgramSetupConfig{" +
                "programId='" + programId + '\'' +
                ", startDate=" + startDate +
                ", workoutDays=" + workoutDays +
                ", remindersEnabled=" + remindersEnabled +
                ", reminderTime=" + reminderTime +
                ", autoProgression=" + autoProgression +
                '}';
    }
} 