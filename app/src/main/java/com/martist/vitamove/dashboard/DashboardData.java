package com.martist.vitamove.dashboard;

import java.util.List;


public class DashboardData {


    private int stepsToday;
    private int stepsGoal;
    private float stepsProgress;
    private List<Integer> stepsHistory;


    private int caloriesGoal;
    private int caloriesConsumed;
    private int caloriesBurned;


    private float waterConsumed;
    private float waterGoal;


    private float currentWeight;
    private float weightChange;
    private List<Float> weightHistory;


    private int proteinConsumed;
    private int fatsConsumed;
    private int carbsConsumed;
    private int proteinGoal;
    private int fatsGoal;
    private int carbsGoal;


    public DashboardData() {

        this.proteinGoal = 120;
        this.fatsGoal = 70;
        this.carbsGoal = 250;
    }


    public int getStepsToday() {
        return stepsToday;
    }

    public void setStepsToday(int stepsToday) {
        this.stepsToday = stepsToday;
    }

    public int getStepsGoal() {
        return stepsGoal;
    }

    public void setStepsGoal(int stepsGoal) {
        this.stepsGoal = stepsGoal;
    }

    public List<Integer> getStepsHistory() {
        return stepsHistory;
    }

    public void setStepsHistory(List<Integer> stepsHistory) {
        this.stepsHistory = stepsHistory;
    }

    public int getCaloriesGoal() {
        return caloriesGoal;
    }

    public void setCaloriesGoal(int caloriesGoal) {
        this.caloriesGoal = caloriesGoal;
    }

    public int getCaloriesConsumed() {
        return caloriesConsumed;
    }

    public void setCaloriesConsumed(int caloriesConsumed) {
        this.caloriesConsumed = caloriesConsumed;
    }

    public int getCaloriesBurned() {
        return caloriesBurned;
    }

    public void setCaloriesBurned(int caloriesBurned) {
        this.caloriesBurned = caloriesBurned;
    }

    public float getWaterConsumed() {
        return waterConsumed;
    }

    public void setWaterConsumed(float waterConsumed) {
        this.waterConsumed = waterConsumed;
    }

    public float getWaterGoal() {
        return waterGoal;
    }

    public void setWaterGoal(float waterGoal) {
        this.waterGoal = waterGoal;
    }

    public float getCurrentWeight() {
        return currentWeight;
    }

    public void setCurrentWeight(float currentWeight) {
        this.currentWeight = currentWeight;
    }

    public float getWeightChange() {
        return weightChange;
    }

    public void setWeightChange(float weightChange) {
        this.weightChange = weightChange;
    }

    public List<Float> getWeightHistory() {
        return weightHistory;
    }

    public void setWeightHistory(List<Float> weightHistory) {
        this.weightHistory = weightHistory;
    }


    public int getProteinConsumed() {
        return proteinConsumed;
    }

    public void setProteinConsumed(int proteinConsumed) {
        this.proteinConsumed = proteinConsumed;
    }

    public int getFatsConsumed() {
        return fatsConsumed;
    }

    public void setFatsConsumed(int fatsConsumed) {
        this.fatsConsumed = fatsConsumed;
    }

    public int getCarbsConsumed() {
        return carbsConsumed;
    }

    public void setCarbsConsumed(int carbsConsumed) {
        this.carbsConsumed = carbsConsumed;
    }

    public int getProteinGoal() {
        return proteinGoal;
    }

    public void setProteinGoal(int proteinGoal) {
        this.proteinGoal = proteinGoal;
    }

    public int getFatsGoal() {
        return fatsGoal;
    }

    public void setFatsGoal(int fatsGoal) {
        this.fatsGoal = fatsGoal;
    }

    public int getCarbsGoal() {
        return carbsGoal;
    }

    public void setCarbsGoal(int carbsGoal) {
        this.carbsGoal = carbsGoal;
    }


    public int getStepsProgress() {
        return (int) ((float) stepsToday / stepsGoal * 100);
    }

    public int getRemainingCalories() {
        return caloriesGoal - caloriesConsumed + caloriesBurned;
    }

    public float getWaterProgress() {
        return waterConsumed / waterGoal;
    }

    public boolean isWeightReducing() {
        return weightChange < 0;
    }


    public List<Integer> getWeeklySteps() {
        return stepsHistory;
    }


    public void setStepsProgress(float stepsProgress) {
        this.stepsProgress = stepsProgress;
    }
} 