package com.martist.vitamove.programs.model;

import com.martist.vitamove.exercise.ui.model.Exercise;

import java.util.ArrayList;
import java.util.List;


public class CreateProgramDay {

    private int dayNumber;
    private String title;

    private List<Exercise> selectedExercises = new ArrayList<>();

    public CreateProgramDay(int dayNumber) {
        this.dayNumber = dayNumber;
        this.title = "День " + dayNumber;
    }

    public int getDayNumber() {
        return dayNumber;
    }

    public void setDayNumber(int dayNumber) {
        this.dayNumber = dayNumber;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }


    public List<Exercise> getSelectedExercises() {
        return selectedExercises;

    }

    public void setSelectedExercises(List<Exercise> selectedExercises) {
        this.selectedExercises = selectedExercises != null ? selectedExercises : new ArrayList<>();

    }


}