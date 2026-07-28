package com.martist.vitamove.workout.domain;


public class WorkoutCompletedEvent {
    private final int caloriesBurned;
    private final long workoutDuration;
    private final String workoutId;

    public WorkoutCompletedEvent(int caloriesBurned, long workoutDuration, String workoutId) {
        this.caloriesBurned = caloriesBurned;
        this.workoutDuration = workoutDuration;
        this.workoutId = workoutId;
    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public long getWorkoutDuration() {
        return workoutDuration;
    }

    public String getWorkoutId() {
        return workoutId;
    }
}