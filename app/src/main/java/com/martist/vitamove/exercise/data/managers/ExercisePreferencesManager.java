package com.martist.vitamove.exercise.data.managers;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;


public class ExercisePreferencesManager {
    private static final String TAG = "ExercisePreferencesManager";
    private static final String PREFS_NAME = "ExerciseSettings";

    private static final String KEY_IS_RESTING = "is_resting_";
    private static final String KEY_REST_TIME = "rest_time_";
    private static final String KEY_IS_SET_ACTIVE = "is_set_active_";
    private static final String KEY_ACTIVE_SET_START_TIME = "active_set_start_time_";
    private static final String KEY_ACTIVE_SET_DURATION = "active_set_duration_";

    private final SharedPreferences preferences;


    public static class SavedState {
        public boolean isResting;
        public long restTime;
        public boolean isSetActive;
        public long activeSetStartTime;
        public long activeSetDuration;

        public SavedState() {
        }
    }

    public ExercisePreferencesManager(Context context) {
        this.preferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
    }


    public void saveRestingState(String exerciseId, boolean isResting, long remainingTime) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_RESTING + exerciseId, isResting);
        editor.putLong(KEY_REST_TIME + exerciseId, remainingTime);
        editor.apply();

        Log.d(TAG, "saveRestingState: exerciseId=" + exerciseId +
                ", isResting=" + isResting + ", remainingTime=" + remainingTime);
    }


    public void saveActiveSetState(String exerciseId, boolean isSetActive, long startTime, long duration) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.putBoolean(KEY_IS_SET_ACTIVE + exerciseId, isSetActive);
        editor.putLong(KEY_ACTIVE_SET_START_TIME + exerciseId, startTime);
        editor.putLong(KEY_ACTIVE_SET_DURATION + exerciseId, duration);
        editor.apply();

        Log.d(TAG, "saveActiveSetState: exerciseId=" + exerciseId +
                ", isSetActive=" + isSetActive + ", duration=" + duration);
    }


    public SavedState loadState(String exerciseId) {
        SavedState state = new SavedState();

        state.isResting = preferences.getBoolean(KEY_IS_RESTING + exerciseId, false);
        state.restTime = preferences.getLong(KEY_REST_TIME + exerciseId, 0);
        state.isSetActive = preferences.getBoolean(KEY_IS_SET_ACTIVE + exerciseId, false);
        state.activeSetStartTime = preferences.getLong(KEY_ACTIVE_SET_START_TIME + exerciseId, 0);
        state.activeSetDuration = preferences.getLong(KEY_ACTIVE_SET_DURATION + exerciseId, 0);

        Log.d(TAG, "loadState: exerciseId=" + exerciseId +
                ", isResting=" + state.isResting + ", isSetActive=" + state.isSetActive);

        return state;
    }


    public void clearState(String exerciseId) {
        SharedPreferences.Editor editor = preferences.edit();
        editor.remove(KEY_IS_RESTING + exerciseId);
        editor.remove(KEY_REST_TIME + exerciseId);
        editor.remove(KEY_IS_SET_ACTIVE + exerciseId);
        editor.remove(KEY_ACTIVE_SET_START_TIME + exerciseId);
        editor.remove(KEY_ACTIVE_SET_DURATION + exerciseId);
        editor.apply();

        Log.d(TAG, "clearState: Очищено состояние для exerciseId=" + exerciseId);
    }


    public void clearAll() {
        preferences.edit().clear().apply();
        Log.d(TAG, "clearAll: Очищены все сохраненные состояния");
    }
}

