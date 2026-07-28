package com.martist.vitamove.programs.ui.model;

import java.util.ArrayList;
import java.util.List;


public class Program {
    private String id;
    private String name;
    private String description;
    private String imageUrl;
    private String level;
    private List<String> goals;
    private int durationWeeks;
    private int workoutsPerWeek;
    private boolean isFavorite;
    private float rating;
    private List<ProgramDay> days;


    public Program() {
        this.goals = new ArrayList<>();
        this.days = new ArrayList<>();
    }


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getImageUrl() {
        return imageUrl;
    }

    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }

    public String getLevel() {
        return level;
    }

    public void setLevel(String level) {
        this.level = level;
    }

    public List<String> getGoals() {
        return goals;
    }

    public void setGoals(List<String> goals) {
        this.goals = goals;
    }

    public void addGoal(String goal) {
        if (this.goals == null) {
            this.goals = new ArrayList<>();
        }
        this.goals.add(goal);
    }

    public int getDurationWeeks() {
        return durationWeeks;
    }

    public void setDurationWeeks(int durationWeeks) {
        this.durationWeeks = durationWeeks;
    }

    public int getWorkoutsPerWeek() {
        return workoutsPerWeek;
    }

    public void setWorkoutsPerWeek(int workoutsPerWeek) {
        this.workoutsPerWeek = workoutsPerWeek;
    }


    public List<ProgramDay> getDays() {
        return days;
    }


    public void setDays(List<ProgramDay> days) {
        this.days = days;
    }


    public void addDay(ProgramDay day) {
        if (this.days == null) {
            this.days = new ArrayList<>();
        }
        this.days.add(day);
    }
} 