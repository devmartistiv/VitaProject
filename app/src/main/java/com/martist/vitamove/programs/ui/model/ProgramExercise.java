package com.martist.vitamove.programs.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.martist.vitamove.exercise.ui.model.Exercise;

import java.util.ArrayList;
import java.util.List;


public class ProgramExercise implements Parcelable {
    private String id;
    private String programDayId;
    private String exerciseId;
    private Exercise exercise;
    private int orderNumber;
    private int restBetweenSetsSec;
    private String notes;
    private List<ProgramSet> sets;
    private String dayId;
    private int targetSets;
    private int targetReps;
    private float targetWeight;
    private long createdAt;
    private long updatedAt;
    private String templateExerciseId;

    public ProgramExercise() {
        sets = new ArrayList<>();
    }


    public String getId() {
        return id;
    }

    public String getProgramDayId() {
        return programDayId;
    }

    public String getExerciseId() {
        return exerciseId;
    }

    public Exercise getExercise() {
        return exercise;
    }

    public int getOrderNumber() {
        return orderNumber;
    }

    public int getRestBetweenSetsSec() {
        return restBetweenSetsSec;
    }

    public String getNotes() {
        return notes;
    }

    public List<ProgramSet> getSets() {
        return sets;
    }

    public String getDayId() {
        return dayId;
    }

    public int getTargetSets() {
        return targetSets;
    }

    public int getTargetReps() {
        return targetReps;
    }

    public float getTargetWeight() {
        return targetWeight;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getTemplateExerciseId() {
        return templateExerciseId;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setProgramDayId(String programDayId) {
        this.programDayId = programDayId;
    }

    public void setExerciseId(String exerciseId) {
        this.exerciseId = exerciseId;
    }

    public void setExercise(Exercise exercise) {
        this.exercise = exercise;
        if (exercise != null) {
            this.exerciseId = exercise.getId();
        }
    }

    public void setOrderNumber(int orderNumber) {
        this.orderNumber = orderNumber;
    }

    public void setRestBetweenSetsSec(int restBetweenSetsSec) {
        this.restBetweenSetsSec = restBetweenSetsSec;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setSets(List<ProgramSet> sets) {
        this.sets = sets != null ? sets : new ArrayList<>();
    }

    public void setDayId(String dayId) {
        this.dayId = dayId;
    }

    public void setTargetSets(int targetSets) {
        this.targetSets = targetSets;
    }

    public void setTargetReps(int targetReps) {
        this.targetReps = targetReps;
    }

    public void setTargetWeight(float targetWeight) {
        this.targetWeight = targetWeight;
    }


    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setTemplateExerciseId(String templateExerciseId) {
        this.templateExerciseId = templateExerciseId;
    }


    public void addSet(ProgramSet set) {
        if (set != null) {
            sets.add(set);
        }
    }

    public void removeSet(ProgramSet set) {
        sets.remove(set);
    }

    public int getSetsCount() {
        return sets.size();
    }


    public int getDurationSeconds() {

        if (exercise != null) {
            return exercise.getDurationSeconds();
        }


        int repsDuration = targetReps * 3;


        int totalDuration = (repsDuration + restBetweenSetsSec) * targetSets - restBetweenSetsSec;


        return Math.max(totalDuration, 30);
    }


    protected ProgramExercise(Parcel in) {
        id = in.readString();
        programDayId = in.readString();
        exerciseId = in.readString();
        exercise = in.readParcelable(Exercise.class.getClassLoader());
        orderNumber = in.readInt();
        restBetweenSetsSec = in.readInt();
        notes = in.readString();
        dayId = in.readString();
        targetSets = in.readInt();
        targetReps = in.readInt();
        targetWeight = in.readFloat();
        createdAt = in.readLong();
        updatedAt = in.readLong();
        templateExerciseId = in.readString();
        sets = new ArrayList<>();
        in.readList(sets, ProgramSet.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(programDayId);
        dest.writeString(exerciseId);
        dest.writeParcelable(exercise, flags);
        dest.writeInt(orderNumber);
        dest.writeInt(restBetweenSetsSec);
        dest.writeString(notes);
        dest.writeString(dayId);
        dest.writeInt(targetSets);
        dest.writeInt(targetReps);
        dest.writeFloat(targetWeight);
        dest.writeLong(createdAt);
        dest.writeLong(updatedAt);
        dest.writeString(templateExerciseId);
        dest.writeList(sets);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProgramExercise> CREATOR = new Creator<ProgramExercise>() {
        @Override
        public ProgramExercise createFromParcel(Parcel in) {
            return new ProgramExercise(in);
        }

        @Override
        public ProgramExercise[] newArray(int size) {
            return new ProgramExercise[size];
        }
    };


    public static class Builder {
        private final ProgramExercise programExercise = new ProgramExercise();

        public Builder id(String id) {
            programExercise.setId(id);
            return this;
        }

        public Builder programDayId(String programDayId) {
            programExercise.setProgramDayId(programDayId);
            return this;
        }

        public Builder exerciseId(String exerciseId) {
            programExercise.setExerciseId(exerciseId);
            return this;
        }

        public Builder exercise(Exercise exercise) {
            programExercise.setExercise(exercise);
            return this;
        }

        public Builder orderNumber(int orderNumber) {
            programExercise.setOrderNumber(orderNumber);
            return this;
        }

        public Builder restBetweenSetsSec(int restBetweenSetsSec) {
            programExercise.setRestBetweenSetsSec(restBetweenSetsSec);
            return this;
        }

        public Builder notes(String notes) {
            programExercise.setNotes(notes);
            return this;
        }

        public Builder sets(List<ProgramSet> sets) {
            programExercise.setSets(sets);
            return this;
        }

        public Builder dayId(String dayId) {
            programExercise.setDayId(dayId);
            return this;
        }

        public Builder targetSets(int targetSets) {
            programExercise.setTargetSets(targetSets);
            return this;
        }

        public Builder targetReps(int targetReps) {
            programExercise.setTargetReps(targetReps);
            return this;
        }

        public Builder targetWeight(float targetWeight) {
            programExercise.setTargetWeight(targetWeight);
            return this;
        }


        public Builder createdAt(long createdAt) {
            programExercise.setCreatedAt(createdAt);
            return this;
        }

        public Builder updatedAt(long updatedAt) {
            programExercise.setUpdatedAt(updatedAt);
            return this;
        }

        public Builder templateExerciseId(String templateExerciseId) {
            programExercise.setTemplateExerciseId(templateExerciseId);
            return this;
        }

        public ProgramExercise build() {
            return programExercise;
        }
    }
} 