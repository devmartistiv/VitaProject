package com.martist.vitamove.exercise.domain;

import com.martist.vitamove.exercise.ui.model.Exercise;


public class AddExerciseEvent {
    public final String exerciseId;
    public final Exercise exercise;

    public AddExerciseEvent(String exerciseId) {
        this(exerciseId, null);
    }

    public AddExerciseEvent(String exerciseId, Exercise exercise) {
        this.exerciseId = exerciseId;
        this.exercise = exercise;
    }
}