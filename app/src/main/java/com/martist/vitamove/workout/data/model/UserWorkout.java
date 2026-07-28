package com.martist.vitamove.workout.data.model;

import java.util.ArrayList;
import java.util.List;

public class UserWorkout {
    private String id;
    private String userId;
    private String name;
    private long startTime;
    private Long endTime;
    private int totalCalories;
    private String notes;
    private String programId;
    private Integer programDayNumber;
    private String programDayId;
    private String planId;
    private List<WorkoutExercise> exercises;


    private int cachedTotalSets = -1;

    public UserWorkout() {
        this.exercises = new ArrayList<>();
    }

    public UserWorkout(String id, String userId, String name, long startTime, Long endTime,
                       int totalCalories, String notes, String programId, Integer programDayNumber,
                       String programDayId, String planId, List<WorkoutExercise> exercises) {
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
        this.exercises = exercises != null ? exercises : new ArrayList<>();
    }


    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public long getStartTime() {
        return startTime;
    }

    public Long getEndTime() {
        return endTime;
    }

    public int getTotalCalories() {
        return totalCalories;
    }

    public String getNotes() {
        return notes;
    }

    public List<WorkoutExercise> getExercises() {
        return exercises;
    }

    public String getProgramId() {
        return programId;
    }

    public Integer getProgramDayNumber() {
        return programDayNumber;
    }

    public String getProgramDayId() {
        return programDayId;
    }

    public String getPlanId() {
        return planId;
    }


    public int getTotalSets() {
        if (cachedTotalSets == -1) {
            calculateTotalSets();
        }
        return cachedTotalSets;
    }


    private void calculateTotalSets() {
        if (exercises == null || exercises.isEmpty()) {
            cachedTotalSets = 0;
            return;
        }

        cachedTotalSets = exercises.stream()
                .mapToInt(e -> {

                    int targetSets = e.getExercise().getDefaultSets();


                    if (e.getSetsCompleted() != null && e.getSetsCompleted().size() > targetSets) {
                        return e.getSetsCompleted().size();
                    }
                    return targetSets > 0 ? targetSets : 0;
                })
                .sum();
    }


    private void invalidateCache() {
        cachedTotalSets = -1;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Long endTime) {
        this.endTime = endTime;
    }

    public void setTotalCalories(int totalCalories) {
        this.totalCalories = totalCalories;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setExercises(List<WorkoutExercise> exercises) {
        this.exercises = exercises != null ? exercises : new ArrayList<>();
        invalidateCache();
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public void setProgramDayNumber(Integer programDayNumber) {
        this.programDayNumber = programDayNumber;
    }

    public void setProgramDayId(String programDayId) {
        this.programDayId = programDayId;
    }

    public void setPlanId(String planId) {
        this.planId = planId;
    }


    public void addExercise(WorkoutExercise exercise) {
        if (exercise != null) {
            exercises.add(exercise);
            invalidateCache();
        }
    }

    public void removeExercise(WorkoutExercise exercise) {
        exercises.remove(exercise);
        invalidateCache();
    }

    public void clearExercises() {
        exercises.clear();
        invalidateCache();
    }

    public int getDurationMinutes() {
        if (endTime != null && endTime > startTime) {
            return (int) ((endTime - startTime) / (1000 * 60));
        } else {
            return 0;
        }
    }


    public boolean isPartOfProgram() {
        return programId != null && programDayNumber != null;
    }
} 