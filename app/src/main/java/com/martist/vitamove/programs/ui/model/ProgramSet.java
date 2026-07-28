package com.martist.vitamove.programs.ui.model;

import android.os.Parcel;
import android.os.Parcelable;


public class ProgramSet implements Parcelable {
    private String id;
    private String programExerciseId;
    private int setNumber;
    private float targetWeight;
    private int targetReps;
    private int targetDurationSec;
    private String setType;


    private int reps;
    private float weight;
    private boolean completed;

    public ProgramSet() {
    }

    public ProgramSet(float targetWeight, int targetReps, int targetDurationSec) {
        this.targetWeight = targetWeight;
        this.targetReps = targetReps;
        this.targetDurationSec = targetDurationSec;
        this.setType = "standard";
        this.reps = targetReps;
        this.weight = targetWeight;
        this.completed = false;
    }


    public String getId() {
        return id;
    }

    public String getProgramExerciseId() {
        return programExerciseId;
    }

    public int getSetNumber() {
        return setNumber;
    }

    public float getTargetWeight() {
        return targetWeight;
    }

    public int getTargetReps() {
        return targetReps;
    }

    public int getTargetDurationSec() {
        return targetDurationSec;
    }

    public String getSetType() {
        return setType;
    }


    public int getReps() {
        return reps;
    }

    public float getWeight() {
        return weight;
    }

    public boolean isCompleted() {
        return completed;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setProgramExerciseId(String programExerciseId) {
        this.programExerciseId = programExerciseId;
    }

    public void setSetNumber(int setNumber) {
        this.setNumber = setNumber;
    }

    public void setTargetWeight(float targetWeight) {
        this.targetWeight = targetWeight;
    }

    public void setTargetReps(int targetReps) {
        this.targetReps = targetReps;
    }

    public void setTargetDurationSec(int targetDurationSec) {
        this.targetDurationSec = targetDurationSec;
    }

    public void setSetType(String setType) {
        this.setType = setType;
    }


    public void setReps(int reps) {
        this.reps = reps;
    }

    public void setWeight(float weight) {
        this.weight = weight;
    }

    public void setCompleted(boolean completed) {
        this.completed = completed;
    }


    protected ProgramSet(Parcel in) {
        id = in.readString();
        programExerciseId = in.readString();
        setNumber = in.readInt();
        targetWeight = in.readFloat();
        targetReps = in.readInt();
        targetDurationSec = in.readInt();
        setType = in.readString();
        reps = in.readInt();
        weight = in.readFloat();
        completed = in.readByte() != 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(programExerciseId);
        dest.writeInt(setNumber);
        dest.writeFloat(targetWeight);
        dest.writeInt(targetReps);
        dest.writeInt(targetDurationSec);
        dest.writeString(setType);
        dest.writeInt(reps);
        dest.writeFloat(weight);
        dest.writeByte((byte) (completed ? 1 : 0));
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProgramSet> CREATOR = new Creator<ProgramSet>() {
        @Override
        public ProgramSet createFromParcel(Parcel in) {
            return new ProgramSet(in);
        }

        @Override
        public ProgramSet[] newArray(int size) {
            return new ProgramSet[size];
        }
    };


    public static class Builder {
        private final ProgramSet programSet = new ProgramSet();

        public Builder id(String id) {
            programSet.setId(id);
            return this;
        }

        public Builder programExerciseId(String programExerciseId) {
            programSet.setProgramExerciseId(programExerciseId);
            return this;
        }

        public Builder setNumber(int setNumber) {
            programSet.setSetNumber(setNumber);
            return this;
        }

        public Builder targetWeight(float targetWeight) {
            programSet.setTargetWeight(targetWeight);
            return this;
        }

        public Builder targetReps(int targetReps) {
            programSet.setTargetReps(targetReps);
            return this;
        }

        public Builder targetDurationSec(int targetDurationSec) {
            programSet.setTargetDurationSec(targetDurationSec);
            return this;
        }

        public Builder setType(String setType) {
            programSet.setSetType(setType);
            return this;
        }

        public Builder reps(int reps) {
            programSet.setReps(reps);
            return this;
        }

        public Builder weight(float weight) {
            programSet.setWeight(weight);
            return this;
        }

        public Builder completed(boolean completed) {
            programSet.setCompleted(completed);
            return this;
        }

        public ProgramSet build() {
            return programSet;
        }
    }
} 