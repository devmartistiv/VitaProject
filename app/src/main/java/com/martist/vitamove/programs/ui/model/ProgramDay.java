package com.martist.vitamove.programs.ui.model;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;


public class ProgramDay implements Parcelable {
    private String id;
    private String programId;
    private int dayNumber;
    private String name;
    private String title;
    private String description;
    private String focusArea;
    private int estimatedDurationMinutes;
    private int durationMinutes;
    private int restBetweenExercisesSec;
    private boolean isCompleted;
    private List<ProgramExercise> exercises;
    private long createdAt;
    private long updatedAt;
    private String templateDayId;
    private transient long plannedTimestamp;
    private String status;


    public ProgramDay() {
        this.exercises = new ArrayList<>();
    }


    public ProgramDay(String id, String programId, int dayNumber, String title, String description, int durationMinutes) {
        this.id = id;
        this.programId = programId;
        this.dayNumber = dayNumber;
        this.title = title;
        this.description = description;
        this.durationMinutes = durationMinutes;
        this.exercises = new ArrayList<>();
    }

    protected ProgramDay(Parcel in) {
        id = in.readString();
        programId = in.readString();
        dayNumber = in.readInt();
        name = in.readString();
        title = in.readString();
        description = in.readString();
        focusArea = in.readString();
        estimatedDurationMinutes = in.readInt();
        durationMinutes = in.readInt();
        restBetweenExercisesSec = in.readInt();
        isCompleted = in.readByte() != 0;
        exercises = new ArrayList<>();
        in.readList(exercises, ProgramExercise.class.getClassLoader());
        createdAt = in.readLong();
        updatedAt = in.readLong();
        templateDayId = in.readString();
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(programId);
        dest.writeInt(dayNumber);
        dest.writeString(name);
        dest.writeString(title);
        dest.writeString(description);
        dest.writeString(focusArea);
        dest.writeInt(estimatedDurationMinutes);
        dest.writeInt(durationMinutes);
        dest.writeInt(restBetweenExercisesSec);
        dest.writeByte((byte) (isCompleted ? 1 : 0));
        dest.writeList(exercises);
        dest.writeLong(createdAt);
        dest.writeLong(updatedAt);
        dest.writeString(templateDayId);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<ProgramDay> CREATOR = new Creator<ProgramDay>() {
        @Override
        public ProgramDay createFromParcel(Parcel in) {
            return new ProgramDay(in);
        }

        @Override
        public ProgramDay[] newArray(int size) {
            return new ProgramDay[size];
        }
    };


    public String getId() {
        return id;
    }

    public String getProgramId() {
        return programId;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public String getName() {
        return name;
    }

    public String getTitle() {
        return title != null ? title : name;
    }

    public String getDescription() {
        return description;
    }

    public String getFocusArea() {
        return focusArea;
    }

    public int getEstimatedDurationMinutes() {
        return estimatedDurationMinutes;
    }

    public int getDurationMinutes() {
        return durationMinutes > 0 ? durationMinutes : estimatedDurationMinutes;
    }

    public int getRestBetweenExercisesSec() {
        return restBetweenExercisesSec;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public List<ProgramExercise> getExercises() {
        return exercises;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public String getTemplateDayId() {
        return templateDayId;
    }

    public long getPlannedTimestamp() {
        return plannedTimestamp;
    }

    public String getStatus() {
        return status;
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public void setName(String name) {
        this.name = name;
        this.title = name;
    }

    public void setTitle(String title) {
        this.title = title;
        this.name = title;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public void setFocusArea(String focusArea) {
        this.focusArea = focusArea != null ? focusArea : "";
    }

    public void setEstimatedDurationMinutes(int estimatedDurationMinutes) {
        this.estimatedDurationMinutes = estimatedDurationMinutes;
        this.durationMinutes = estimatedDurationMinutes;
    }

    public void setDurationMinutes(int durationMinutes) {
        this.durationMinutes = durationMinutes;
        this.estimatedDurationMinutes = durationMinutes;
    }

    public void setRestBetweenExercisesSec(int restBetweenExercisesSec) {
        this.restBetweenExercisesSec = restBetweenExercisesSec;
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void setExercises(List<ProgramExercise> exercises) {
        this.exercises = exercises != null ? exercises : new ArrayList<>();
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setTemplateDayId(String templateDayId) {
        this.templateDayId = templateDayId;
    }

    public void setPlannedTimestamp(long plannedTimestamp) {
        this.plannedTimestamp = plannedTimestamp;
    }

    public void setStatus(String status) {
        this.status = status;
    }


    public void addExercise(ProgramExercise exercise) {
        if (this.exercises == null) {
            this.exercises = new ArrayList<>();
        }
        this.exercises.add(exercise);
    }

    public void removeExercise(ProgramExercise exercise) {
        if (this.exercises != null) {
            this.exercises.remove(exercise);
        }
    }

    public void removeExercise(int index) {
        if (this.exercises != null && index >= 0 && index < this.exercises.size()) {
            this.exercises.remove(index);
        }
    }

    public void calculateDuration() {
        int totalDuration = 0;

        if (this.exercises != null) {
            for (ProgramExercise exercise : this.exercises) {
                totalDuration += exercise.getDurationSeconds();
            }
        }


        this.durationMinutes = (int) Math.ceil(totalDuration / 60.0);
    }


    public static class Builder {
        private final ProgramDay programDay = new ProgramDay();

        public Builder id(String id) {
            programDay.setId(id);
            return this;
        }

        public Builder programId(String programId) {
            programDay.setProgramId(programId);
            return this;
        }

        public Builder dayNumber(int dayNumber) {
            programDay.setDayNumber(dayNumber);
            return this;
        }

        public Builder title(String title) {
            programDay.setTitle(title);
            return this;
        }

        public Builder description(String description) {
            programDay.setDescription(description);
            return this;
        }

        public Builder durationMinutes(int durationMinutes) {
            programDay.setDurationMinutes(durationMinutes);
            return this;
        }

        public Builder exercises(List<ProgramExercise> exercises) {
            programDay.setExercises(exercises);
            return this;
        }

        public ProgramDay build() {
            return programDay;
        }
    }
} 