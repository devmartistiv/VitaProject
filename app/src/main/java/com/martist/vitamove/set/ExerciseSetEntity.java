package com.martist.vitamove.set;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Ignore;
import androidx.room.Index;
import androidx.room.PrimaryKey;

import com.martist.vitamove.exercise.ui.model.ExerciseSet;
import com.martist.vitamove.workout.data.entities.WorkoutExerciseEntity;

@Entity(tableName = "exercise_sets",
        foreignKeys = @ForeignKey(entity = WorkoutExerciseEntity.class,
                parentColumns = "id",
                childColumns = "workout_exercise_id",
                onDelete = ForeignKey.CASCADE),
        indices = {@Index(value = "workout_exercise_id")})
public class ExerciseSetEntity {

    @PrimaryKey
    @NonNull
    private String id;

    @ColumnInfo(name = "set_number")
    private int setNumber;

    private Float weight;
    private Integer reps;

    private boolean completed;

    @NonNull
    @ColumnInfo(name = "workout_exercise_id")
    private String workoutExerciseId;


    @ColumnInfo(name = "exercise_id")
    private String exerciseId;


    @ColumnInfo(name = "duration_seconds")
    private Integer durationSeconds;


    @ColumnInfo(name = "created_at")
    private Long createdAt;


    public ExerciseSetEntity() {
    }


    @Ignore
    public ExerciseSetEntity(@NonNull String id, int setNumber, Float weight, Integer reps, boolean completed,
                             @NonNull String workoutExerciseId, String exerciseId, Integer durationSeconds) {
        this.id = id;
        this.setNumber = setNumber;
        this.weight = weight;
        this.reps = reps;
        this.completed = completed;
        this.workoutExerciseId = workoutExerciseId;
        this.exerciseId = exerciseId;
        this.durationSeconds = durationSeconds;
        this.createdAt = System.currentTimeMillis();
    }


    @NonNull
    public String getId() {
        return id;
    }

    public void setId(@NonNull String id) {
        this.id = id;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public Float getWeight() {
        return weight;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public Integer getReps() {
        return reps;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public boolean isCompleted() {
        return completed;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    @NonNull
    public String getWorkoutExerciseId() {
        return workoutExerciseId;
    }

    public void setWorkoutExerciseId(@NonNull String workoutExerciseId) {
        this.workoutExerciseId = workoutExerciseId;
    }


    public String getExerciseId() {
        return exerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }


    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }


    public Long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }


    public ExerciseSet toModel() {
        ExerciseSet model = new ExerciseSet();
        model.setId(this.id);
        model.setSetNumber(this.setNumber);
        model.setWeight(this.weight);
        model.setReps(this.reps);
        model.setCompleted(this.completed);
        model.setWorkoutExerciseId(this.workoutExerciseId);
        model.setExerciseId(this.exerciseId);
        model.setDurationSeconds(this.durationSeconds);
        if (this.createdAt != null) {
            model.setCreatedAt(this.createdAt);
        }
        return model;
    }


    public static ExerciseSetEntity fromModel(ExerciseSet model, @NonNull String workoutExerciseId) {
        if (model.getId() == null) {

            model.setId(java.util.UUID.randomUUID().toString());
        }
        ExerciseSetEntity entity = new ExerciseSetEntity(
                model.getId(),
                model.getSetNumber(),
                model.getWeight(),
                model.getReps(),
                model.isCompleted(),
                workoutExerciseId,
                model.getExerciseId(),
                model.getDurationSeconds()
        );


        if (model.getCreatedAt() != null) {
            entity.setCreatedAt(model.getCreatedAt());
        } else {
            entity.setCreatedAt(System.currentTimeMillis());
        }

        return entity;
    }
} 