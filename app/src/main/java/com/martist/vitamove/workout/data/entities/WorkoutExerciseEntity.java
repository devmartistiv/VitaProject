package com.martist.vitamove.workout.data.entities;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.workout.data.model.WorkoutExercise;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Entity(tableName = "workout_exercises",
        foreignKeys = @ForeignKey(entity = UserWorkoutEntity.class,
                parentColumns = "id",
                childColumns = "workout_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "workout_id")}

)
public class WorkoutExerciseEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "order_number")
    private int orderNumber;

    private String notes;

    @ColumnInfo(name = "start_time")
    private Date startTime;

    @ColumnInfo(name = "end_time")
    private Date endTime;

    @ColumnInfo(name = "is_rated")
    private boolean isRated;

    @NonNull
    @ColumnInfo(name = "workout_id")
    private String workoutId;

    @NonNull
    @ColumnInfo(name = "base_exercise_id")
    private String baseExerciseId;

    @ColumnInfo(name = "superset_id")
    private String superset_id;

    @ColumnInfo(name = "superset_order")
    private int superset_order;


    public WorkoutExerciseEntity() {
    }


    @Ignore
    public WorkoutExerciseEntity(@NonNull String id, int orderNumber, String notes, Date startTime, Date endTime,
                                 boolean isRated, @NonNull String workoutId, @NonNull String baseExerciseId) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.notes = notes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isRated = isRated;
        this.workoutId = workoutId;
        this.baseExerciseId = baseExerciseId;
    }

    @Ignore
    public WorkoutExerciseEntity(@NonNull String id, int orderNumber, String notes, Date startTime, Date endTime,
                                 boolean isRated, @NonNull String workoutId, @NonNull String baseExerciseId, String superset_id, int superset_order) {
        this.id = id;
        this.orderNumber = orderNumber;
        this.notes = notes;
        this.startTime = startTime;
        this.endTime = endTime;
        this.isRated = isRated;
        this.workoutId = workoutId;
        this.baseExerciseId = baseExerciseId;
        this.superset_id = superset_id;
        this.superset_order = superset_order;
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public String getNotes() {
        return notes != null ? notes : "";

    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public Date getStartTime() {
        return startTime;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public boolean isRated() {
        return isRated;
    }

    public void setRated(boolean rated) {
        isRated = rated;
    }

    @NonNull
    public String getWorkoutId() {
        return workoutId;
    }

    public String getSuperset_id() {
        return superset_id;
    }

    public int getSuperset_order() {
        return superset_order;
    }

    public void setWorkoutId(@NonNull String workoutId) {
        this.workoutId = workoutId;
    }

    @NonNull
    public String getBaseExerciseId() {
        return baseExerciseId;
    }

    public void setBaseExerciseId(@NonNull String baseExerciseId) {
        this.baseExerciseId = baseExerciseId;
    }

    public void setSuperset_id(String superset_id) {
        this.superset_id = superset_id;
    }

    public void setSuperset_order(int superset_order) {
        this.superset_order = superset_order;
    }


    public WorkoutExercise toModel(Exercise baseExerciseDetails, List<ExerciseSet> sets) {
        WorkoutExercise model = new WorkoutExercise();
        model.setId(this.id);
        model.setExercise(baseExerciseDetails);
        model.setOrderNumber(this.orderNumber);
        model.setSetsCompleted(sets != null ? sets : new ArrayList<>());
        model.setNotes(this.notes);
        model.setStartTime(this.startTime);
        model.setEndTime(this.endTime);
        model.setRated(this.isRated);
        model.setSuperset_id(this.superset_id);
        model.setSuperset_order(this.superset_order);
        model.setWorkoutId(workoutId);

        return model;
    }


    public static WorkoutExerciseEntity fromModel(WorkoutExercise model, @NonNull String workoutId) {
        if (model.getId() == null) {
            model.setId(java.util.UUID.randomUUID().toString());
        }
        if (model.getExercise() == null || model.getExercise().getId() == null) {
            throw new IllegalArgumentException("Base Exercise ID cannot be null in WorkoutExercise model");
        }
        return new WorkoutExerciseEntity(
                model.getId(),
                model.getOrderNumber(),
                model.getNotes(),
                model.getStartTime(),
                model.getEndTime(),
                model.isRated(),
                workoutId,
                model.getExercise().getId(),
                model.getSuperset_id(),
                model.getSuperset_order()
        );
    }
} 