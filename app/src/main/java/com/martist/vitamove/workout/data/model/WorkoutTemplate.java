package com.martist.vitamove.workout.data.model;

import com.martist.vitamove.exercise.ui.model.Exercise;

import java.util.ArrayList;
import java.util.List;

public class WorkoutTemplate {
    private String id;
    private String name;
    private String userId;
    private List<Exercise> exercises;
    private String notes;


    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public String getUserId() {
        return userId;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public String getNotes() {
        return notes;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises != null ? exercises : new ArrayList<>();
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

} 