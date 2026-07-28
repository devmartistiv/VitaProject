package com.martist.vitamove.workout.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.exercise.ui.model.ExerciseSet;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class WorkoutExercise implements Parcelable {
    private String id;
    private String workoutId;
    private Exercise exercise;
    private int orderNumber;
    private List<ExerciseSet> setsCompleted;
    private String notes;
    private Date startTime;
    private Date endTime;
    private boolean isRated;
    private boolean isCompleted;
    private String superset_id;
    private int superset_order;

    public WorkoutExercise() {
        this.setsCompleted = new ArrayList<>();
        this.isRated = false;
        this.isCompleted = false;
    }

    protected WorkoutExercise(Parcel in) {
        id = in.readString();
        exercise = in.readParcelable(Exercise.class.getClassLoader());
        orderNumber = in.readInt();
        setsCompleted = new ArrayList<>();
        in.readList(setsCompleted, ExerciseSet.class.getClassLoader());
        notes = in.readString();
        long tmpStartTime = in.readLong();
        startTime = tmpStartTime != -1 ? new Date(tmpStartTime) : null;
        long tmpEndTime = in.readLong();
        endTime = tmpEndTime != -1 ? new Date(tmpEndTime) : null;
        isRated = in.readByte() != 0;
        isCompleted = in.readByte() != 0;
        superset_id = in.readString();
        superset_order = in.readInt();
        workoutId = in.readString();

    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeParcelable(exercise, flags);
        dest.writeInt(orderNumber);
        dest.writeList(setsCompleted);
        dest.writeString(notes);
        dest.writeLong(startTime != null ? startTime.getTime() : -1);
        dest.writeLong(endTime != null ? endTime.getTime() : -1);
        dest.writeByte((byte) (isRated ? 1 : 0));
        dest.writeByte((byte) (isCompleted ? 1 : 0));
        dest.writeString(superset_id);
        dest.writeInt(superset_order);
        dest.writeString(workoutId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WorkoutExercise> CREATOR = new Creator<WorkoutExercise>() {
        @Override
        public WorkoutExercise createFromParcel(Parcel in) {
            return new WorkoutExercise(in);
        }

        @Override
        public WorkoutExercise[] newArray(int size) {
            return new WorkoutExercise[size];
        }
    };

    public WorkoutExercise(WorkoutExercise template) {
        this.exercise = template.getExercise();
        this.orderNumber = template.getOrderNumber();
        this.setsCompleted = new ArrayList<>(template.getSetsCompleted());
        this.notes = template.getNotes();
        this.isRated = template.isRated();
        this.isCompleted = template.isCompleted();
        this.superset_id = template.superset_id;
        this.superset_order = template.superset_order;
        this.workoutId = template.workoutId;

    }

    public WorkoutExercise(String id, Exercise exercise, int orderNumber,
                           List<ExerciseSet> setsCompleted, String notes) {
        this.id = id;
        this.exercise = exercise;
        this.orderNumber = orderNumber;
        this.setsCompleted = setsCompleted != null ? setsCompleted : new ArrayList<>();
        this.notes = notes;
        this.isRated = false;
        this.isCompleted = false;


    }

    public WorkoutExercise(String id, Exercise exercise, int orderNumber,
                           List<ExerciseSet> setsCompleted, String notes, String superset_id, int superset_order) {
        this.id = id;
        this.exercise = exercise;
        this.orderNumber = orderNumber;
        this.setsCompleted = setsCompleted != null ? setsCompleted : new ArrayList<>();
        this.notes = notes;
        this.isRated = false;
        this.isCompleted = false;
        this.superset_order = superset_order;
        this.superset_id = superset_id;
    }

    public WorkoutExercise(String id, Exercise exercise, int orderNumber,
                           List<ExerciseSet> setsCompleted, String notes, String superset_id, int superset_order, String workoutId) {
        this.id = id;
        this.exercise = exercise;
        this.orderNumber = orderNumber;
        this.setsCompleted = setsCompleted != null ? setsCompleted : new ArrayList<>();
        this.notes = notes;
        this.isRated = false;
        this.isCompleted = false;
        this.superset_order = superset_order;
        this.superset_id = superset_id;
        this.workoutId = workoutId;
    }


    public String getId() {
        return id;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public List<ExerciseSet> getSetsCompleted() {
        return setsCompleted;
    }

    public String getNotes() {
        return notes;
    }

    public Date getStartTime() {
        return startTime;
    }

    public Date getEndTime() {
        return endTime;
    }

    public boolean isRated() {
        return isRated;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public List<Exercise> getExercises() {
        List<Exercise> exercises = new ArrayList<>();
        if (exercise != null) {
            exercises.add(exercise);
        }
        return exercises;
    }

    public String getSuperset_id() {
        return superset_id;
    }

    public int getSuperset_order() {
        return superset_order;
    }

    public String getWorkoutId() {
        return workoutId;
    }

    public void setWorkoutId(String workoutId) {
        this.workoutId = workoutId;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setSetsCompleted(List<ExerciseSet> setsCompleted) {
        this.setsCompleted = setsCompleted != null ? setsCompleted : new ArrayList<>();
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setStartTime(Date startTime) {
        this.startTime = startTime;
    }

    public void setEndTime(Date endTime) {
        this.endTime = endTime;
    }

    public void setRated(boolean rated) {
        this.isRated = rated;
    }

    public void setCompleted(boolean completed) {
        this.isCompleted = completed;
    }

    public void setSuperset_id(String superset_id) {
        this.superset_id = superset_id;
    }

    public void setSuperset_order(int superset_order) {
        this.superset_order = superset_order;
    }


    public void addSet(ExerciseSet set) {
        if (set != null) {
            setsCompleted.add(set);
        }
    }

    public void removeSet(ExerciseSet set) {
        setsCompleted.remove(set);
    }

    public void clearSets() {
        setsCompleted.clear();
    }

    public int getCompletedSetsCount() {
        if (setsCompleted == null || setsCompleted.isEmpty()) {
            return 0;
        }


        int count = 0;
        for (ExerciseSet set : setsCompleted) {
            boolean isCompleted = set.isCompleted();
            if (isCompleted) {
                count++;
            }

            Log.d("WorkoutExercise", "Подход #" + set.getSetNumber() +
                    ", ID: " + set.getId() +
                    ", Выполнен: " + isCompleted +
                    ", Вес: " + set.getWeight() +
                    ", Повторения: " + set.getReps());
        }


        Log.d("WorkoutExercise", "Всего подходов: " + setsCompleted.size() +
                ", Выполненных подходов: " + count);

        return count;
    }


    public void updateCompletedSetsCount() {


    }
} 