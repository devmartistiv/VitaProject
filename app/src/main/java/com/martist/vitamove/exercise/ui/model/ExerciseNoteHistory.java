package com.martist.vitamove.exercise.ui.model;

import java.util.Date;


public class ExerciseNoteHistory {
    private String noteText;
    private Date workoutDate;
    private String workoutName;
    private String workoutTime;
    private int setsCompleted;
    private long exerciseDurationMinutes;
    private String exerciseId;
    private String workoutId;

    public ExerciseNoteHistory() {
    }

    public ExerciseNoteHistory(String noteText, Date workoutDate, String workoutName,
                               String workoutTime, int setsCompleted, long exerciseDurationMinutes,
                               String exerciseId, String workoutId) {
        this.noteText = noteText;
        this.workoutDate = workoutDate;
        this.workoutName = workoutName;
        this.workoutTime = workoutTime;
        this.setsCompleted = setsCompleted;
        this.exerciseDurationMinutes = exerciseDurationMinutes;
        this.exerciseId = exerciseId;
        this.workoutId = workoutId;
    }


    public String getNoteText() {
        return noteText;
    }

    public void setNoteText(String noteText) {
        this.noteText = noteText;
    }

    public Date getWorkoutDate() {
        return workoutDate;
    }

    public void setWorkoutDate(Date workoutDate) {
        this.workoutDate = workoutDate;
    }

    public String getWorkoutName() {
        return workoutName;
    }

    public void setWorkoutName(String workoutName) {
        this.workoutName = workoutName;
    }

    public String getWorkoutTime() {
        return workoutTime;
    }

    public void setWorkoutTime(String workoutTime) {
        this.workoutTime = workoutTime;
    }

    public int getSetsCompleted() {
        return setsCompleted;
    }

    public void setSetsCompleted(int setsCompleted) {
        this.setsCompleted = setsCompleted;
    }

    public long getExerciseDurationMinutes() {
        return exerciseDurationMinutes;
    }

    public void setExerciseDurationMinutes(long exerciseDurationMinutes) {
        this.exerciseDurationMinutes = exerciseDurationMinutes;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public String getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(String workoutId) {
        this.workoutId = workoutId;
    }
}
