package com.martist.vitamove.user;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import androidx.preference.PreferenceManager;

import com.martist.vitamove.VitaMoveApplication;


public class UserProfile {
    private String name;
    private int age;
    private String gender;
    private float currentWeight;
    private float targetWeight;
    private float height;
    private float bodyFat;
    private float waist;
    private int targetCalories;
    private float targetWater;


    public UserProfile(String name, int age, String gender, float currentWeight,
                       float targetWeight, float height, float bodyFat, float waist) {
        this.name = name;
        this.age = age;
        this.gender = gender;
        this.currentWeight = currentWeight;
        this.targetWeight = targetWeight;
        this.height = height;
        this.bodyFat = bodyFat;
        this.waist = waist;
        this.targetCalories = calculateTargetCalories();
        this.targetWater = calculateTargetWater();
    }


    public String getName() {
        return name;
    }


    public void setName(String name) {
        this.name = name;
    }


    public int getAge() {
        return age;
    }


    public void setAge(int age) {
        this.age = age;
    }


    public String getGender() {
        return gender;
    }


    public void setGender(String gender) {
        this.gender = gender;
    }


    public float getCurrentWeight() {
        return currentWeight;
    }


    public void setCurrentWeight(float currentWeight) {
        this.currentWeight = currentWeight;
    }


    public float getTargetWeight() {
        return targetWeight;
    }


    public void setTargetWeight(float targetWeight) {
        this.targetWeight = targetWeight;
    }


    public float getHeight() {
        return height;
    }


    public void setHeight(float height) {
        this.height = height;
    }


    public float getBodyFat() {
        return bodyFat;
    }


    public void setBodyFat(float bodyFat) {
        this.bodyFat = bodyFat;
    }


    public int getTargetCalories() {
        return targetCalories;
    }


    public void setTargetCalories(int targetCalories) {
        this.targetCalories = targetCalories;
    }


    public void updateTargetCalories() {
        this.targetCalories = calculateTargetCalories();
    }


    public int calculateTargetCalories() {

        boolean isMale = this.gender.equalsIgnoreCase("Мужчина");
        float bmr;

        if (isMale) {
            bmr = 10 * this.currentWeight + 6.25f * this.height - 5 * this.age + 5;
        } else {
            bmr = 10 * this.currentWeight + 6.25f * this.height - 5 * this.age - 161;
        }


        SharedPreferences prefs = null;
        SharedPreferences userDataPrefs = null;

        try {

            Context context = VitaMoveApplication.getAppContext();
            if (context != null) {

                prefs = PreferenceManager.getDefaultSharedPreferences(context);
                userDataPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
            }
        } catch (Exception e) {

            Log.e("UserProfile", "Не удалось получить контекст для доступа к настройкам", e);
        }


        float activityFactor = 1.375f;


        String activityLevel = "intermediate";
        if (userDataPrefs != null) {
            activityLevel = userDataPrefs.getString("fitness_level", null);
        }


        if (activityLevel == null && prefs != null) {
            activityLevel = prefs.getString("user_fitness_level", "intermediate");
        }


        if (activityLevel == null) {
            activityLevel = "intermediate";
        }

        Log.d("UserProfile", "Расчет калорий использует fitness_level: " + activityLevel);


        switch (activityLevel) {
            case "beginner":

                activityFactor = 1.2f;
                break;
            case "intermediate":

                activityFactor = 1.375f;
                break;
            case "advanced":

                activityFactor = 1.55f;
                break;
            case "expert":

                activityFactor = 1.725f;
                break;
        }


        if (age < 18) {

            activityFactor *= 1.1f;
        } else if (age > 50) {

            activityFactor *= 0.95f;
        }


        float dailyCalories = bmr * activityFactor;


        String fitnessGoal = "weight_loss";


        if (userDataPrefs != null) {
            fitnessGoal = userDataPrefs.getString("fitness_goal", null);
        }


        if (fitnessGoal == null && prefs != null) {
            fitnessGoal = prefs.getString("fitness_goal", "weight_loss");
        }


        if (fitnessGoal == null) {
            fitnessGoal = "weight_loss";
        }

        Log.d("UserProfile", "Расчет калорий использует fitness_goal: " + fitnessGoal);


        float weightDifference = Math.abs(this.currentWeight - this.targetWeight);
        float adjustmentFactor = 1.0f;


        switch (fitnessGoal) {
            case "weight_loss":

                dailyCalories -= 500;
                break;
            case "muscle_gain":

                dailyCalories += 500;
                break;
            case "endurance":


                dailyCalories += 250;
                break;
            case "general_fitness":
            default:


                break;
        }


        if (isMale) {
            dailyCalories = Math.max(dailyCalories, 1700);
        } else {
            dailyCalories = Math.max(dailyCalories, 1500);
        }


        Log.d("UserProfile", "Расчет калорий: BMR=" + bmr +
                ", activityFactor=" + activityFactor +
                ", goal=" + fitnessGoal +
                ", dailyCalories=" + Math.round(dailyCalories));

        return Math.round(dailyCalories);
    }


    public float getWaist() {
        return waist;
    }


    public void setWaist(float waist) {
        this.waist = waist;
    }


    public float getTargetWater() {
        return targetWater;
    }


    public void setTargetWater(float targetWater) {
        this.targetWater = targetWater;
    }


    public void updateTargetWater() {
        this.targetWater = calculateTargetWater();
    }


    public float calculateTargetWater() {

        float baseWaterNeeds = this.currentWeight * 0.03f;


        SharedPreferences prefs = null;
        SharedPreferences userDataPrefs = null;
        float activityFactor = 1.0f;

        try {

            Context context = VitaMoveApplication.getAppContext();
            if (context != null) {

                prefs = PreferenceManager.getDefaultSharedPreferences(context);
                userDataPrefs = context.getSharedPreferences("user_data", Context.MODE_PRIVATE);
            }
        } catch (Exception e) {
            Log.e("UserProfile", "Не удалось получить контекст для доступа к настройкам", e);
        }


        String fitnessLevel = "intermediate";
        if (userDataPrefs != null) {
            fitnessLevel = userDataPrefs.getString("fitness_level", null);
        }


        if (fitnessLevel == null && prefs != null) {
            fitnessLevel = prefs.getString("user_fitness_level", "intermediate");
        }


        if (fitnessLevel == null) {
            fitnessLevel = "intermediate";
        }

        Log.d("UserProfile", "Расчет воды использует fitness_level: " + fitnessLevel);


        switch (fitnessLevel) {
            case "beginner":

                activityFactor = 1.1f;
                break;
            case "intermediate":

                activityFactor = 1.2f;
                break;
            case "advanced":

                activityFactor = 1.4f;
                break;
        }


        String fitnessGoal = "weight_loss";


        if (userDataPrefs != null) {
            fitnessGoal = userDataPrefs.getString("fitness_goal", null);
        }


        if (fitnessGoal == null && prefs != null) {
            fitnessGoal = prefs.getString("fitness_goal", "weight_loss");
        }


        if (fitnessGoal == null) {
            fitnessGoal = "weight_loss";
        }

        Log.d("UserProfile", "Расчет воды использует fitness_goal: " + fitnessGoal);

        float goalFactor = 1.0f;
        switch (fitnessGoal) {
            case "weight_loss":

                goalFactor = 1.1f;
                break;
            case "muscle_gain":

                goalFactor = 1.05f;
                break;
            case "endurance":

                goalFactor = 1.15f;
                break;
            default:
                goalFactor = 1.0f;
                break;
        }


        float recommendedWater = baseWaterNeeds * activityFactor * goalFactor;


        recommendedWater = Math.round(recommendedWater * 10) / 10.0f;


        return Math.max(recommendedWater, 1.5f);
    }
} 