package com.martist.vitamove.exercise.ui.model;

public enum ExerciseType {
    strength("Силовое"),
    cardio("Кардио"),
    flexibility("Растяжка");

    private final String russianName;

    ExerciseType(String russianName) {
        this.russianName = russianName;
    }

    public String getRussianName() {
        return russianName;
    }
} 