package com.martist.vitamove.workout.data.model;

import androidx.annotation.NonNull;

import com.martist.vitamove.exercise.ui.model.Exercise;

import java.util.Date;
import java.util.List;
import java.util.UUID;


public class Workout {

    public static final int STATUS_PLANNED = 0;
    public static final int STATUS_IN_PROGRESS = 1;
    public static final int STATUS_COMPLETED = 2;
    public static final int STATUS_MISSED = 3;


    public static final int TYPE_GENERAL = 0;
    public static final int TYPE_CARDIO = 1;
    public static final int TYPE_STRENGTH = 2;
    public static final int TYPE_FLEXIBILITY = 3;
    public static final int TYPE_HIIT = 4;

    private final String id;
    private final String programId;
    private String name;
    private int type;
    private Date scheduledDate;
    private int durationMinutes;
    private int status;
    private int caloriesBurned;
    private List<Exercise> exercises;


    public Workout(String programId, String name, int type) {
        this.id = UUID.randomUUID().toString();
        this.programId = programId;
        this.name = name;
        this.type = type;
        this.status = STATUS_PLANNED;
        this.durationMinutes = 0;
        this.caloriesBurned = 0;
    }


    public String getId() {
        return id;
    }


    public String getProgramId() {
        return programId;
    }


    public String getName() {
        return name;
    }


    public String getTitle() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public int getType() {
        return type;
    }


    public void setType(int type) {
        this.type = type;
    }


    public Date getScheduledDate() {
        return scheduledDate;
    }


    public void setScheduledDate(Date scheduledDate) {
        this.scheduledDate = scheduledDate;
    }


    public int getDurationMinutes() {
        return durationMinutes;
    }


    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
    }


    public int getStatus() {
        return status;
    }


    public void setStatus(int status) {
        this.status = status;
    }


    public int getCaloriesBurned() {
        return caloriesBurned;
    }


    public void setCaloriesBurned(int caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }


    public List<Exercise> getExercises() {
        return exercises;
    }


    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises;
    }


    public int getExercisesCount() {
        return exercises != null ? exercises.size() : 0;
    }


    public void addExercise(Exercise exercise) {
        if (exercises != null) {
            exercises.add(exercise);
        }
    }


    public boolean removeExercise(Exercise exercise) {
        return exercises != null && exercises.remove(exercise);
    }


    public void start() {
        if (status == STATUS_PLANNED) {
            status = STATUS_IN_PROGRESS;
        }
    }


    public void complete(int caloriesBurned) {
        this.status = STATUS_COMPLETED;
        this.caloriesBurned = caloriesBurned;
    }


    public void markAsMissed() {
        if (status == STATUS_PLANNED && scheduledDate != null && scheduledDate.before(new Date())) {
            status = STATUS_MISSED;
        }
    }


    @NonNull
    @Override
    public String toString() {
        return "Workout{" +
                "id='" + id + '\'' +
                ", name='" + name + '\'' +
                ", status=" + status +
                ", scheduledDate=" + scheduledDate +
                '}';
    }
} 