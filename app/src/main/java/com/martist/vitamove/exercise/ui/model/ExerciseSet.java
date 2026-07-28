package com.martist.vitamove.exercise.ui.model;

import android.os.Parcel;
import android.os.Parcelable;


public class ExerciseSet implements Parcelable {
    private String id;
    private Float weight;
    private Integer reps;
    private Integer durationSeconds;
    private boolean completed;
    private int setNumber;
    private String workoutExerciseId;
    private String exerciseId;
    private Integer targetReps;
    private Float targetWeight;
    private Long createdAt;

    public ExerciseSet() {
        this(null, null, null, false, 1);
        this.id = java.util.UUID.randomUUID().toString();
        this.createdAt = System.currentTimeMillis();
    }

    public ExerciseSet(Float weight, Integer reps, Integer durationSeconds, boolean completed) {
        this(weight, reps, durationSeconds, completed, 1);
    }

    public ExerciseSet(Float weight, Integer reps, Integer durationSeconds, boolean completed, int setNumber) {
        this.id = java.util.UUID.randomUUID().toString();
        this.weight = weight;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.completed = completed;
        this.setNumber = setNumber;
        this.createdAt = System.currentTimeMillis();
    }


    public ExerciseSet(ExerciseSet other) {
        if (other != null) {
            this.id = other.id != null ? other.id : java.util.UUID.randomUUID().toString();
            this.weight = other.weight;
            this.reps = other.reps;
            this.durationSeconds = other.durationSeconds;
            this.completed = other.completed;
            this.setNumber = other.setNumber;
            this.workoutExerciseId = other.workoutExerciseId;
            this.exerciseId = other.exerciseId;
            this.targetReps = other.targetReps;
            this.targetWeight = other.targetWeight;
            this.createdAt = other.createdAt != null ? other.createdAt : System.currentTimeMillis();
        } else {
            this.id = java.util.UUID.randomUUID().toString();
            this.createdAt = System.currentTimeMillis();
        }
    }


    public ExerciseSet(String id, Float weight, Integer reps, boolean completed, String workoutExerciseId, int setNumber) {
        this.id = id;
        this.weight = weight;
        this.reps = reps;
        this.completed = completed;
        this.workoutExerciseId = workoutExerciseId;
        this.setNumber = setNumber;
        this.createdAt = System.currentTimeMillis();
    }


    public ExerciseSet(String id, Float weight, Integer reps, Integer durationSeconds, boolean completed,
                       String workoutExerciseId, String exerciseId, int setNumber) {
        this.id = id;
        this.weight = weight;
        this.reps = reps;
        this.durationSeconds = durationSeconds;
        this.completed = completed;
        this.workoutExerciseId = workoutExerciseId;
        this.exerciseId = exerciseId;
        this.setNumber = setNumber;
        this.createdAt = System.currentTimeMillis();
    }

    protected ExerciseSet(Parcel in) {
        if (in.readByte() == 0) {
            weight = null;
        } else {
            weight = in.readFloat();
        }
        if (in.readByte() == 0) {
            reps = null;
        } else {
            reps = in.readInt();
        }
        if (in.readByte() == 0) {
            durationSeconds = null;
        } else {
            durationSeconds = in.readInt();
        }
        completed = in.readByte() != 0;
        setNumber = in.readInt();
        workoutExerciseId = in.readString();
        exerciseId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        if (weight == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeFloat(weight);
        }
        if (reps == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(reps);
        }
        if (durationSeconds == null) {
            dest.writeByte((byte) 0);
        } else {
            dest.writeByte((byte) 1);
            dest.writeInt(durationSeconds);
        }
        dest.writeByte((byte) (completed ? 1 : 0));
        dest.writeInt(setNumber);
        dest.writeString(workoutExerciseId);
        dest.writeString(exerciseId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ExerciseSet> CREATOR = new Creator<ExerciseSet>() {
        @Override
        public ExerciseSet createFromParcel(Parcel in) {
            return new ExerciseSet(in);
        }

        @Override
        public ExerciseSet[] newArray(int size) {
            return new ExerciseSet[size];
        }
    };


    public String getId() {

        if (id == null) {
            id = java.util.UUID.randomUUID().toString();
        }
        return id;
    }

    public Integer getReps() {
        return reps;
    }

    public Integer getDurationSeconds() {
        return durationSeconds;
    }

    public boolean isCompleted() {
        return completed;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public Float getWeight() {
        return weight;
    }

    public String getWorkoutExerciseId() {
        return workoutExerciseId;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public Integer getTargetReps() {
        return targetReps;
    }

    public Float getTargetWeight() {
        return targetWeight;
    }

    public Long getCreatedAt() {
        return createdAt;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setReps(Integer reps) {
        this.reps = reps;
    }

    public void setDurationSeconds(Integer durationSeconds) {
        this.durationSeconds = durationSeconds;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public void setWeight(Float weight) {
        this.weight = weight;
    }

    public void setWorkoutExerciseId(String workoutExerciseId) {
        this.workoutExerciseId = workoutExerciseId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public void setTargetReps(Integer targetReps) {
        this.targetReps = targetReps;
    }

    public void setTargetWeight(Float targetWeight) {
        this.targetWeight = targetWeight;
    }

    public void setCreatedAt(Long createdAt) {
        this.createdAt = createdAt;
    }


    public boolean isTimeBasedSet() {
        return durationSeconds != null && durationSeconds > 0;
    }

    public boolean isWeightBasedSet() {
        return weight != null && weight > 0;
    }


    public static class Builder {
        private int setNumber;
        private String workoutExerciseId;
        private String exerciseId;
        private float weight;
        private int reps;
        private int durationSeconds;
        private boolean isCompleted;
        private int targetReps;
        private float targetWeight;

        public Builder setNumber(int setNumber) {
            this.setNumber = setNumber;
            return this;
        }

        public Builder workoutExerciseId(String workoutExerciseId) {
            this.workoutExerciseId = workoutExerciseId;
            return this;
        }

        public Builder exerciseId(String exerciseId) {
            this.exerciseId = exerciseId;
            return this;
        }

        public Builder weight(float weight) {
            this.weight = weight;
            return this;
        }

        public Builder reps(int reps) {
            this.reps = reps;
            return this;
        }

        public Builder durationSeconds(int durationSeconds) {
            this.durationSeconds = durationSeconds;
            return this;
        }

        public Builder isCompleted(boolean isCompleted) {
            this.isCompleted = isCompleted;
            return this;
        }

        public Builder targetReps(int targetReps) {
            this.targetReps = targetReps;
            return this;
        }

        public Builder targetWeight(float targetWeight) {
            this.targetWeight = targetWeight;
            return this;
        }

        public ExerciseSet build() {
            ExerciseSet set = new ExerciseSet();
            set.setNumber = this.setNumber;
            set.workoutExerciseId = this.workoutExerciseId;
            set.exerciseId = this.exerciseId;
            set.weight = this.weight;
            set.reps = this.reps;
            set.durationSeconds = this.durationSeconds;
            set.completed = this.isCompleted;
            set.targetReps = this.targetReps;
            set.targetWeight = this.targetWeight;
            return set;
        }
    }
} 