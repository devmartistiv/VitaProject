package com.martist.vitamove.workout.domain;

import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.List;


public class WorkoutRepeatEvent {
    private final List<WorkoutExercise> exercises;

    public WorkoutRepeatEvent(List<WorkoutExercise> exercises) {
        this.exercises = exercises;
    }

    public List<WorkoutExercise> getExercises() {
        return exercises;
    }
}
