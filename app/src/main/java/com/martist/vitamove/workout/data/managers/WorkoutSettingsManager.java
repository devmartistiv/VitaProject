package com.martist.vitamove.workout.data.managers;

import android.content.Context;
import android.content.SharedPreferences;


public class WorkoutSettingsManager {
    private static final String PREFS_NAME = "workout_settings";
    private static final String KEY_TIMER_SECONDS = "rest_timer_seconds";
    private static final String KEY_AUTO_NEXT_EXERCISE = "auto_next_exercise";
    private static final String KEY_REST_TIMER_ENABLED = "rest_timer_enabled";


    private static final int DEFAULT_TIMER_SECONDS = 60;
    private static final boolean DEFAULT_AUTO_NEXT_EXERCISE = false;
    private static final boolean DEFAULT_REST_TIMER_ENABLED = true;

    private final SharedPreferences sharedPreferences;


    private static WorkoutSettingsManager instance;


    public static synchronized WorkoutSettingsManager getInstance(Context context) {
        if (instance == null) {
            instance = new WorkoutSettingsManager(context.getApplicationContext());
        }
        return instance;
    }


    private WorkoutSettingsManager(Context context) {
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public int getRestTimerSeconds() {
        return sharedPreferences.getInt(KEY_TIMER_SECONDS, DEFAULT_TIMER_SECONDS);
    }


    public void setRestTimerSeconds(int seconds) {
        sharedPreferences.edit().putInt(KEY_TIMER_SECONDS, seconds).apply();
    }


    public boolean isRestTimerEnabled() {
        return sharedPreferences.getBoolean(KEY_REST_TIMER_ENABLED, DEFAULT_REST_TIMER_ENABLED);
    }


    public void setRestTimerEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_REST_TIMER_ENABLED, enabled).apply();
    }


    public boolean isAutoNextExerciseEnabled() {
        return sharedPreferences.getBoolean(KEY_AUTO_NEXT_EXERCISE, DEFAULT_AUTO_NEXT_EXERCISE);
    }


    public void setAutoNextExerciseEnabled(boolean enabled) {
        sharedPreferences.edit().putBoolean(KEY_AUTO_NEXT_EXERCISE, enabled).apply();
    }
} 