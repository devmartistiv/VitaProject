package com.martist.vitamove.workout.domain;


public class WorkoutStartedEvent {
    private final long startTime;

    public WorkoutStartedEvent(long startTime) {
        this.startTime = startTime;
    }

    public long getStartTime() {
        return startTime;
    }
} 