package com.martist.vitamove.exercise.data.remote.model;

import static com.martist.vitamove.workout.data.repository.SupabaseWorkoutRepository.ISO_DATE_FORMAT;

import com.google.gson.annotations.SerializedName;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;

public class ExerciseDto {

    private String id;
    @SerializedName("exercise_id")
    private String baseExerciseId;
    @SerializedName("order_number")
    private int orderNumber;

    private String notes;

    @SerializedName("start_time")
    private String startTime;

    @SerializedName("end_time")
    private String endTime;

    @SerializedName("is_rated")
    private boolean isRated;

    @SerializedName("workout_id")
    private String workoutId;


    @SerializedName("superset_id")
    private String superset_id;

    @SerializedName("superset_order")
    private int superset_order;
    private String created_at;

    public ExerciseDto(WorkoutExerciseEntity exercise, String current_time) {
        id = exercise.getId();
        workoutId = exercise.getWorkoutId();
        baseExerciseId = exercise.getBaseExerciseId();
        orderNumber = exercise.getOrderNumber();
        notes = exercise.getNotes();
        startTime = exercise.getStartTime() != null ? ISO_DATE_FORMAT.format(exercise.getStartTime()) : null;
        endTime = exercise.getEndTime() != null ?
                ISO_DATE_FORMAT.format(exercise.getEndTime()) : null;
        isRated = exercise.isRated();
        superset_id = exercise.getSuperset_id() != null ? exercise.getSuperset_id() : null;
        superset_order = exercise.getSuperset_order();
        created_at = current_time;

    }
}
