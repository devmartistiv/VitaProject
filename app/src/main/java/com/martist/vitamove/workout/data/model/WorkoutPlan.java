package com.martist.vitamove.workout.data.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.programs.ui.model.ProgramDay;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;


public class WorkoutPlan implements Parcelable {
    private String id;
    private String userId;
    private String name;
    private long plannedDate;
    private String programId;
    private String programDayId;
    private String status;
    private String notes;
    private long createdAt;
    private long updatedAt;
    private List<Exercise> plannedExercises;
    private boolean isCompleted;
    private boolean isMissed;


    private WorkoutProgram program;
    private ProgramDay programDay;

    public WorkoutPlan() {
        this.plannedExercises = new ArrayList<>();
    }

    public WorkoutPlan(String id, String userId, String name, Date plannedDate,
                       List<Exercise> plannedExercises, boolean isCompleted,
                       boolean isMissed, String notes) {
        this.id = id;
        this.userId = userId;
        this.name = name;
        this.plannedDate = plannedDate != null ? plannedDate.getTime() : 0;
        this.plannedExercises = plannedExercises != null ? plannedExercises : new ArrayList<>();
        this.isCompleted = isCompleted;
        this.isMissed = isMissed;
        this.notes = notes;
    }


    public String getId() {
        return id;
    }

    public String getUserId() {
        return userId;
    }

    public String getName() {
        return name;
    }

    public long getPlannedDate() {
        return plannedDate;
    }

    public Date getPlannedDateObject() {
        return new Date(plannedDate);
    }

    public List<Exercise> getPlannedExercises() {
        return plannedExercises;
    }

    public boolean isCompleted() {
        return isCompleted;
    }

    public boolean isMissed() {
        return isMissed;
    }

    public String getNotes() {
        return notes;
    }

    public String getProgramId() {
        return programId;
    }

    public String getProgramDayId() {
        return programDayId;
    }

    public String getStatus() {
        return status;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public WorkoutProgram getProgram() {
        return program;
    }

    public ProgramDay getProgramDay() {
        return programDay;
    }

    public Date getCreatedAtDate() {
        return new Date(createdAt);
    }

    public Date getUpdatedAtDate() {
        return new Date(updatedAt);
    }


    public void setId(String id) {
        this.id = id;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPlannedDate(long plannedDate) {
        this.plannedDate = plannedDate;
    }

    public void setPlannedExercises(List<Exercise> plannedExercises) {
        this.plannedExercises = plannedExercises != null ? plannedExercises : new ArrayList<>();
    }

    public void setCompleted(boolean completed) {
        isCompleted = completed;
    }

    public void setMissed(boolean missed) {
        isMissed = missed;
    }

    public void setNotes(String notes) {
        this.notes = notes;
    }

    public void setProgramId(String programId) {
        this.programId = programId;
    }

    public void setProgramDayId(String programDayId) {
        this.programDayId = programDayId;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setProgram(WorkoutProgram program) {
        this.program = program;
    }


    protected WorkoutPlan(Parcel in) {
        id = in.readString();
        userId = in.readString();
        name = in.readString();
        plannedDate = in.readLong();
        programId = in.readString();
        programDayId = in.readString();
        status = in.readString();
        notes = in.readString();
        createdAt = in.readLong();
        updatedAt = in.readLong();
        isCompleted = in.readByte() != 0;
        isMissed = in.readByte() != 0;
        plannedExercises = new ArrayList<>();
        in.readList(plannedExercises, Exercise.class.getClassLoader());
        program = in.readParcelable(WorkoutProgram.class.getClassLoader());
        programDay = in.readParcelable(ProgramDay.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeLong(plannedDate);
        dest.writeString(programId);
        dest.writeString(programDayId);
        dest.writeString(status);
        dest.writeString(notes);
        dest.writeLong(createdAt);
        dest.writeLong(updatedAt);
        dest.writeByte((byte) (isCompleted ? 1 : 0));
        dest.writeByte((byte) (isMissed ? 1 : 0));
        dest.writeList(plannedExercises);
        dest.writeParcelable(program, flags);
        dest.writeParcelable(programDay, flags);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WorkoutPlan> CREATOR = new Creator<WorkoutPlan>() {
        @Override
        public WorkoutPlan createFromParcel(Parcel in) {
            return new WorkoutPlan(in);
        }

        @Override
        public WorkoutPlan[] newArray(int size) {
            return new WorkoutPlan[size];
        }
    };


    public static class Builder {
        private String id;
        private String userId;
        private String name;
        private Date plannedDate;
        private List<Exercise> plannedExercises = new ArrayList<>();
        private boolean isCompleted;
        private boolean isMissed;
        private String notes;

        public Builder id(String id) {
            this.id = id;
            return this;
        }

        public Builder userId(String userId) {
            this.userId = userId;
            return this;
        }

        public Builder name(String name) {
            this.name = name;
            return this;
        }

        public Builder plannedDate(Date plannedDate) {
            this.plannedDate = plannedDate;
            return this;
        }

        public Builder plannedExercises(List<Exercise> plannedExercises) {
            this.plannedExercises = plannedExercises;
            return this;
        }

        public Builder isCompleted(boolean isCompleted) {
            this.isCompleted = isCompleted;
            return this;
        }

        public Builder isMissed(boolean isMissed) {
            this.isMissed = isMissed;
            return this;
        }

        public Builder notes(String notes) {
            this.notes = notes;
            return this;
        }

        public WorkoutPlan build() {
            return new WorkoutPlan(id, userId, name, plannedDate, plannedExercises,
                    isCompleted, isMissed, notes);
        }
    }


    public String getTitle() {
        return name;
    }
} 