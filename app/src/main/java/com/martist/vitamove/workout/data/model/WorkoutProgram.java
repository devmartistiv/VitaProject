package com.martist.vitamove.workout.data.model;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.Log;

import com.martist.vitamove.exercise.ui.model.Exercise;
import com.martist.vitamove.programs.ui.model.ProgramDay;
import com.martist.vitamove.programs.ui.model.ProgramType;

import java.util.ArrayList;
import java.util.List;


public class WorkoutProgram implements Parcelable {
    private String id;
    private String userId;
    private String name;
    private String description;
    private String imageUrl;
    private String level;
    private String goal;
    private int durationWeeks;
    private int daysPerWeek;
    private ProgramType type;
    private String programType;
    private String author;


    private long startDate;
    private long endDate;
    private int completedWorkouts;
    private boolean isActive;
    private boolean isFavorite;
    private float userRating;
    private long createdAt;
    private long updatedAt;


    private List<Integer> workoutDays;

    private List<ProgramDay> days;
    private List<Exercise> exercises;

    public WorkoutProgram() {
        days = new ArrayList<>();
        exercises = new ArrayList<>();
        workoutDays = new ArrayList<>();
        type = ProgramType.OFFICIAL;
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

    public String getDescription() {
        return description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public String getLevel() {
        return level;
    }

    public String getGoal() {
        return goal;
    }

    public String getProgramType() {
        return programType;
    }

    public int getDurationWeeks() {
        return durationWeeks;
    }

    public int getDaysPerWeek() {
        return daysPerWeek;
    }

    public long getStartDate() {
        return startDate;
    }

    public boolean isActive() {
        return isActive;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public long getUpdatedAt() {
        return updatedAt;
    }

    public List<ProgramDay> getDays() {
        return days;
    }

    public List<Exercise> getExercises() {
        return exercises;
    }

    public ProgramType getType() {
        return type;
    }

    public List<Integer> getWorkoutDays() {

        if (workoutDays == null || workoutDays.isEmpty()) {
            Log.d("WorkoutProgram", "!!! VITAMOVE_DEBUG: [PROGRAM " + id + "] Дни тренировок не заданы или пустые, возвращаем по умолчанию (0, 2, 4)");
            List<Integer> defaultDays = new ArrayList<>();
            defaultDays.add(0);
            defaultDays.add(2);
            defaultDays.add(4);
            return defaultDays;
        }
        Log.d("WorkoutProgram", "!!! VITAMOVE_DEBUG: [PROGRAM " + id + "] Возвращаем сохраненные дни тренировок: " + workoutDays);
        return workoutDays;
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

    public void setDescription(String description) {
        this.description = description;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public void setGoal(String goal) {
        this.goal = goal;
    }

    public void setProgramType(String programType) {
        this.programType = programType;
    }

    public void setDurationWeeks(int durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    public void setDaysPerWeek(int daysPerWeek) {
        this.daysPerWeek = daysPerWeek;
    }

    public void setStartDate(long startDate) {
        this.startDate = startDate;
    }

    public void setCompletedWorkouts(int completedWorkouts) {
        this.completedWorkouts = completedWorkouts;
    }

    public void setActive(boolean active) {
        isActive = active;
    }

    public void setFavorite(boolean favorite) {
        isFavorite = favorite;
    }

    public void setUserRating(float userRating) {
        this.userRating = userRating;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(long updatedAt) {
        this.updatedAt = updatedAt;
    }

    public void setDays(List<ProgramDay> days) {
        this.days = days != null ? days : new ArrayList<>();
    }

    public void setType(ProgramType type) {
        this.type = type != null ? type : ProgramType.OFFICIAL;
    }

    public void setAuthor(String author) {
        this.author = author;
    }

    public void setEndDate(long endDate) {
        this.endDate = endDate;
    }

    public void setExercises(List<Exercise> exercises) {
        this.exercises = exercises != null ? new ArrayList<>(exercises) : new ArrayList<>();
    }

    public void setWorkoutDays(List<Integer> workoutDays) {
        Log.d("WorkoutProgram", "!!! VITAMOVE_DEBUG: [PROGRAM " + id + "] Устанавливаем дни тренировок: " + workoutDays);
        if (workoutDays == null || workoutDays.isEmpty()) {
            Log.w("WorkoutProgram", "!!! VITAMOVE_DEBUG: [PROGRAM " + id + "] Попытка установить пустые или null дни тренировок");
            this.workoutDays = new ArrayList<>();
            return;
        }
        this.workoutDays = workoutDays;
    }


    protected WorkoutProgram(Parcel in) {
        id = in.readString();
        userId = in.readString();
        name = in.readString();
        description = in.readString();
        imageUrl = in.readString();
        level = in.readString();
        goal = in.readString();
        programType = in.readString();
        durationWeeks = in.readInt();
        daysPerWeek = in.readInt();
        startDate = in.readLong();
        completedWorkouts = in.readInt();
        isActive = in.readByte() != 0;
        isFavorite = in.readByte() != 0;
        userRating = in.readFloat();
        createdAt = in.readLong();
        updatedAt = in.readLong();
        days = new ArrayList<>();
        in.readList(days, ProgramDay.class.getClassLoader());
        exercises = new ArrayList<>();
        in.readList(exercises, Exercise.class.getClassLoader());
        type = ProgramType.valueOf(in.readString());
        author = in.readString();
        endDate = in.readLong();
        workoutDays = new ArrayList<>();
        in.readList(workoutDays, Integer.class.getClassLoader());
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(id);
        dest.writeString(userId);
        dest.writeString(name);
        dest.writeString(description);
        dest.writeString(imageUrl);
        dest.writeString(level);
        dest.writeString(goal);
        dest.writeString(programType);
        dest.writeInt(durationWeeks);
        dest.writeInt(daysPerWeek);
        dest.writeLong(startDate);
        dest.writeInt(completedWorkouts);
        dest.writeByte((byte) (isActive ? 1 : 0));
        dest.writeByte((byte) (isFavorite ? 1 : 0));
        dest.writeFloat(userRating);
        dest.writeLong(createdAt);
        dest.writeLong(updatedAt);
        dest.writeList(days);
        dest.writeList(exercises);
        dest.writeString(type.name());
        dest.writeString(author);
        dest.writeLong(endDate);
        dest.writeList(workoutDays);
    }

    @Override
    public int describeContents() {
        return 0;
    }

    public static final Creator<WorkoutProgram> CREATOR = new Creator<WorkoutProgram>() {
        @Override
        public WorkoutProgram createFromParcel(Parcel in) {
            return new WorkoutProgram(in);
        }

        @Override
        public WorkoutProgram[] newArray(int size) {
            return new WorkoutProgram[size];
        }
    };


    public static class Builder {
        private final WorkoutProgram program = new WorkoutProgram();

        public Builder id(String id) {
            program.setId(id);
            return this;
        }

        public Builder userId(String userId) {
            program.setUserId(userId);
            return this;
        }

        public Builder name(String name) {
            program.setName(name);
            return this;
        }

        public Builder description(String description) {
            program.setDescription(description);
            return this;
        }

        public Builder imageUrl(String imageUrl) {
            program.setImageUrl(imageUrl);
            return this;
        }

        public Builder level(String level) {
            program.setLevel(level);
            return this;
        }

        public Builder goal(String goal) {
            program.setGoal(goal);
            return this;
        }

        public Builder durationWeeks(int durationWeeks) {
            program.setDurationWeeks(durationWeeks);
            return this;
        }

        public Builder daysPerWeek(int daysPerWeek) {
            program.setDaysPerWeek(daysPerWeek);
            return this;
        }

        public Builder startDate(long startDate) {
            program.setStartDate(startDate);
            return this;
        }

        public Builder isActive(boolean isActive) {
            program.setActive(isActive);
            return this;
        }


        public Builder days(List<ProgramDay> days) {
            program.setDays(days);
            return this;
        }

        public Builder exercises(List<Exercise> exercises) {
            program.setExercises(exercises);
            return this;
        }

        public Builder type(ProgramType type) {
            program.setType(type);
            return this;
        }


        public WorkoutProgram build() {
            return program;
        }
    }


} 